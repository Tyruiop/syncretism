(ns datops-backend.handler
  (:require
   [clojure.string :as str]
   [clojure.data.json :as json]
   [compojure.core :refer :all]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [taoensso.timbre :as timbre :refer [info warn error]]
   [taoensso.timbre.appenders.core :as appenders]
   [clojure.java.jdbc :as db]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as jdbp]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [syncretism.time :refer [market-time]]))

(timbre/merge-config!
 {:appenders {:println {:enabled? false}
              :spit (appenders/spit-appender {:fname "opts-backend.log"})}})

(def db (-> "resources/db.edn" slurp read-string))

(defn get-count [] (-> (db/query db "SELECT COUNT(*) FROM live") first vals first))

(defn get-quotes [symbs]
  (try
    (with-open [con (jdbc/get-connection db)]
      (jdbc/execute!
       con
       (into
        [(str "SELECT * FROM live_quote WHERE "
              (str/join
               " OR "
               (map (fn [symb] "symbol=?") symbs)))]
        symbs)))
    (catch Exception e [])))

(defn get-contract [contract]
  (try
    (with-open [con (jdbc/get-connection db)]
      (jdbc/execute!
       con
       ["SELECT * FROM live WHERE contractSymbol = ?" contract]))
    (catch Exception e [])))

(defn get-expirations [ticker]
  (try
    (with-open [con (jdbc/get-connection db)]
      (jdbc/execute!
       con
       ["SELECT DISTINCT(expiration) FROM live WHERE symbol = ?" ticker]))
    (catch Exception e [])))

(defn get-fundamentals [symbs]
  (try
    (with-open [con (jdbc/get-connection db)]
      (jdbc/execute!
       con
       (into
        [(str "SELECT * FROM fundamentals WHERE "
              (str/join
               " OR "
               (map (fn [symb] "symbol=?") symbs)))]
        symbs)))
    (catch Exception e [])))

(defn get-timeseries [contract]
  (let [contract (str/replace contract #"[^A-Z0-9]" "")]
    (info (str "--- timeseries request " contract))
    (try
      (with-open [con (jdbc/get-connection db)]
        (jdbc/execute!
         con
         ["SELECT * FROM timeseries WHERE contractsymbol=?" contract]))
      (catch Exception e []))))

(defn get-catalysts
  [symbs]
  (let [d (get-fundamentals symbs)
        data (map
              (juxt :fundamentals/symbol (comp json/read-str :fundamentals/data))
              d)]
    (->> data
         (map (fn [[ticker d]]
                (let [events (get d "calendarEvents")]
                  [ticker
                   {"earnings" (get-in events ["earnings" "earningsDate"])
                    "dividends" (get events "dividendDate")}])))
         (into {}))))

(defn get-ladder
  "Given a (ticker, opt-type, expiration) input, returns all corresponding
  options, to allow for vertical spread calculations."
  [ticker opttype expiration]
  (info (str "--- Ladder request " ticker " " expiration " " opttype))
  (try
    (with-open [con (jdbc/get-connection db)]
      (->> (jdbc/execute!
            con
            ["SELECT * FROM live WHERE symbol=?  AND expiration=? AND opttype = ?"
             ticker expiration opttype])
           (map
            (fn [d]
              (->> d
                   (map (fn [[k v]] [(-> k name keyword) v]))
                   (into {}))))
           doall))
    (catch Exception e [])))

(defn get-option-chain
  "Given a (ticker, expiration) input, returns the corresponding
  options chain."
  [ticker expiration]
  (info (str "--- Chain request " ticker " " expiration))
  (try
    (with-open [con (jdbc/get-connection db)]
      (->> (jdbc/execute!
            con
            ["SELECT contractSymbol, opttype FROM live WHERE symbol=? AND expiration=?"
             ticker expiration])
           (map
            (fn [d]
              (->> d
                   (map (fn [[k v]] [(-> k name keyword) v]))
                   (into {}))))
           (group-by :opttype)
           (map (fn [[k v]] [k (map :contractSymbol v)]))
           (into {})
           doall))
    (catch Exception e [])))

(def order-aliases
  {"e_desc" "expiration desc"
   "e_asc" "expiration asc"
   "s_desc" "strike desc"
   "s_asc" "strike asc"
   "t_desc" "symbol desc"
   "t_asc" "symbol asc"
   "iv_desc" "impliedvolatility desc"
   "iv_asc" "impliedvolatility asc"
   "oi_desc" "openinterest desc"
   "oi_asc" "openinterest asc"
   "v_desc" "volume desc"
   "v_asc" "volume asc"
   "lp_desc" "bid desc"
   "lp_asc" "bid asc"
   "md_desc" "regularmarketprice desc"
   "md_asc" "regularmarketprice asc"
   "mdh_desc" "regularmarketdayhigh desc"
   "mdh_asc" "regularmarketdayhigh asc"
   "mdl_desc" "regularmarketdaylow desc"
   "mdl_asc" "regularmarketdaylow asc"})

(defn parse-tickers
  [tickers]
  (let [tick (-> tickers
                 str/upper-case
                 (str/split #"[ ,]+"))]
    ;; Remove empty tickers
    (filter #(not= % "") tick)))

(defn run-query [{:keys [tickers exclude
                         min-diff max-diff itm otm
                         min-ask-bid max-ask-bid
                         min-exp max-exp
                         min-price max-price
                         min-iv max-iv
                         min-oi max-oi
                         min-volume max-volume
                         min-voi max-voi
                         min-strike max-strike
                         min-stock max-stock
                         calls puts
                         stock etf
                         min-sto max-sto
                         min-yield max-yield
                         min-myield max-myield
                         min-delta max-delta
                         min-gamma max-gamma
                         min-theta max-theta
                         min-vega max-vega
                         min-cap max-cap
                         order-by limit offset active]
                  :as req
                  :or {etf true stock true itm true otm true puts true calls true}}]
  (let [order-column (get order-aliases order-by "impliedvolatility desc")
        tickers (if tickers (parse-tickers tickers) "")
        cur-time (int (/ (System/currentTimeMillis) 1000))
        params '()
        params (conj params (if (number? min-exp)
                              (+ cur-time (* min-exp 3600 24))
                              cur-time))
        params (if max-exp
                 (conj params (+ cur-time (* max-exp 3600 24)))
                 params)
        params (if (> (count tickers) 0)
                 (into params tickers)
                 params)
        params (if min-diff
                 (into params [(float (- 1 (/ min-diff 100)))
                               (float (+ 1 (/ min-diff 100)))])
                 params)
        params (if max-diff
                 (into params [(float (- 1 (/ max-diff 100)))
                               (float (+ 1 (/ max-diff 100)))])
                 params)
        params (if min-ask-bid (conj params min-ask-bid) params)
        params (if max-ask-bid (conj params max-ask-bid) params)
        params (if max-price (into params [max-price max-price]) params)
        params (into params [(if min-price min-price 0.001) (if min-price min-price 0.001)])
        params (if min-iv (conj params min-iv) params)
        params (if max-iv (conj params max-iv) params)
        params (if min-oi (conj params min-oi) params)
        params (if max-oi (conj params max-oi) params)
        params (if min-volume (conj params min-volume) params)
        params (if max-volume (conj params max-volume) params)
        params (if min-voi (conj params min-voi) params)
        params (if max-voi (conj params max-voi) params)
        params (if min-sto (conj params min-sto) params)
        params (if max-sto (conj params max-sto) params)
        params (if min-strike (conj params min-strike) params)
        params (if max-strike (conj params max-strike) params)
        params (if min-stock (conj params min-stock) params)
        params (if max-stock (conj params max-stock) params)
        params (if min-yield (conj params min-yield) params)
        params (if max-yield (conj params max-yield) params)
        params (if min-myield (conj params min-myield) params)
        params (if max-myield (conj params max-myield) params)
        params (if min-delta (conj params min-delta) params)
        params (if max-delta (conj params max-delta) params)
        params (if min-gamma (conj params min-gamma) params)
        params (if max-gamma (conj params max-gamma) params)
        params (if min-theta (conj params min-theta) params)
        params (if max-theta (conj params max-theta) params)
        params (if min-vega (conj params min-vega) params)
        params (if max-vega (conj params max-vega) params)
        params (if min-cap (conj params min-cap) params)
        params (if max-cap (conj params max-cap) params)
        params (into params
                     [(- (int (/ (System/currentTimeMillis) 1000))
                         (* 24 3600 21))
                      (- (int (/ (System/currentTimeMillis) 1000))
                         (* 24 3600 3))
                      (if (number? offset)
                        offset
                        0)
                      (if (number? limit)
                        limit
                        100)])
        params (into [] (reverse params))
        
        query
        (str
         ;; Start query
         "SELECT * FROM live"
         ;; If we require market cap, then we must do a join
         (when (or min-cap max-cap)
           " LEFT JOIN live_quote ON live_quote.symbol = live.symbol")
         " WHERE"

         ;; Minimum expiration date (do not consider expired options (start with this
         ;; to be sure to need AND after)
         " expiration > ?"
         
         ;; Max expiration date
         (when max-exp
           " AND expiration <= ?")

         ;; Ticker selection
         (when (> (count tickers) 0)
           (str
            " AND ("
            (if exclude
              (str/join " AND " (map (fn [_] "live.symbol <> ?") tickers))
              (str/join " OR " (map (fn [_] "live.symbol = ?") tickers)))
            ")"))

         ;; Stock to strike diff
         (when min-diff
           " AND ((strike <= ? * regularmarketprice) OR (strike >= ? * regularmarketprice))")
         (when max-diff
           " AND strike >= ? * regularmarketprice AND strike <= ? * regularmarketprice")

         ;; ITM/OTM
         (when (not itm)
           " AND inTheMoney <> true")
         (when (not otm)
           " AND inTheMoney = true")

         ;; Ask bid spread
         (when min-ask-bid
           " AND ask - bid >= ?")
         (when max-ask-bid
           " AND ask - bid <= ?")

         ;; Premium (put a minimum price by default not to show options with no premium
         (when max-price
           (str " AND ((ask <> 0 AND ask <= ? "
                ") OR (ask = 0 AND lastprice <= ?))"))
         (str
          " AND ((ask <> 0 AND ask >= ?"
          ;; Here we do this because sometimes, yahoo resets all ask prices to 0
          ;; this allows us to check last price if it is the case
          ") OR (ask = 0 AND lastprice >= ?"
          "))")

         ;; IV
         (when min-iv
           " AND impliedvolatility >= ?")
         (when max-iv
           " AND impliedvolatility <= ?")
         
         ;; Open Interest
         (when min-oi
           " AND openInterest >= ?")
         (when max-oi
           " AND openInterest <= ?")

         ;; Volume
         (when min-volume
           " AND volume >= ?")
         (when max-volume
           " AND volume <= ?")

         ;; Volume / OI ratio
         (when min-voi
           " AND volume / openInterest >= ?")
         (when max-voi
           " AND volume / openInterest <= ?")

         ;; Stock/Option price ratio
         (when min-sto
           " AND (100*ask)/regularmarketprice >= ?")
         (when max-sto
           " AND (100*ask)/regularmarketprice <= ?")

         ;; Strike price
         (when min-strike
           " AND strike >= ?")
         (when max-strike
           " AND strike <= ?")

         ;; Underlying stock price
         (when min-stock
           " AND regularmarketprice >= ?")
         (when max-stock
           " AND regularmarketprice <= ?")

         ;; Yield
         (when min-yield
           " AND yield >= ?")
         (when max-yield
           " AND yield <= ?")

         ;; Monthly yield
         (when min-myield
           " AND monthlyyield >= ?")
         (when max-myield
           " AND monthlyyield <= ?")

         ;; Greeks
         (when min-delta
           " AND delta >= ?")
         (when max-delta
           " AND delta <= ?")
         (when min-gamma
           " AND gamma >= ?")
         (when max-gamma
           " AND gamma <= ?")
         (when min-theta
           " AND theta >= ?")
         (when max-theta
           " AND theta <= ?")
         (when min-vega
           " AND vega >= ?")
         (when max-vega
           " AND vega <= ?")

         ;; Market cap
         (when min-cap
           " AND JSON_VALUE(live_quote.data, '$.marketCap') >= ?")
         (when max-cap
           " AND JSON_VALUE(live_quote.data, '$.marketCap') <= ?")
         
         ;; Opt-type
         (when (not puts) " AND opttype <> 'P'")
         (when (not calls) " AND opttype <> 'C'")

         ;; Stock type
         (cond (and etf (not stock)) " AND quotetype = 'ETF'"
               (and stock (not etf)) " AND quotetype <> 'ETF'"
               :else "")

         (when active
           " AND ask > 0 AND bid > 0 AND volume > 0 AND openinterest > 0")
         )

        ;; Last traded in the last week (old "active" flag, by default for the moment)
        query (str query " AND lasttradedate > ? AND lastcrawl > ?")
        query (str query " ORDER BY " order-column)
        query (str query " LIMIT ?, ?")]
    (info "--- Received:" req " → " query " → " params)
    (try
      (with-open [con (jdbc/get-connection db)]
        (->> (jdbc/execute! con (into [query] params))
             (map
              (fn [d]
                (->> d
                     (map (fn [[k v]] [(-> k name keyword) v]))
                     (into {}))))
             doall))
      (catch Exception _ {:error "Error in query."}))))

(defroutes app-routes
  (GET "/" [] "Hi :)")
  (GET "/index.html" req (pr-str req))
  (GET "/count" []
       (let [res (get-count)]
         (pr-str {:opts-nb res})))
  (GET "/ops" req
       (let [res (-> req :body slurp (json/read-str :key-fn keyword) run-query)]
         (json/write-str res)))
  (POST "/ops" req
        (let [res (-> req :body slurp (json/read-str :key-fn keyword) run-query)
              symbols (map :symbol res)
              quotes (->> symbols
                          get-quotes
                          (map
                           (fn [{symb :live_quote/symbol data :live_quote/data}]
                             [symb (json/read-str data :key-fn keyword)]))
                          (into {}))             
              catalysts (get-catalysts symbols)]
          (json/write-str {:options res :quotes quotes :catalysts catalysts})))
  (GET "/ops/:cs" [cs] (json/write-str (first (get-contract cs))))
  (GET "/ops/chain/:ticker/:expiration"
       [ticker expiration]
       (let [q-res (get-option-chain ticker expiration)]
         (json/write-str q-res)))
  (GET "/ops/ladder/:ticker/:opttype/:expiration"
       [ticker opttype expiration]
       (let [q-res (get-ladder ticker opttype expiration)]
         (json/write-str q-res)))
  (GET "/ops/expirations/:ticker"
       [ticker]
       (let [q-res (get-expirations ticker)]
         (json/write-str q-res)))
  (GET "/catalysts/:ticker"
       [ticker]
       (json/write-str (get-catalysts [ticker])))
  (GET "/ops/historical/:contract" [contract]
       (json/write-str (get-timeseries contract)))
  (GET "/market/status" req (pr-str {:status (market-time (System/currentTimeMillis))}))
  (route/not-found "Not Found"))

(def app
  (->
   (handler/site app-routes)
   (wrap-cors :access-control-allow-origin [#".*"]
              :access-control-allow-methods [:get :put :post :delete])))

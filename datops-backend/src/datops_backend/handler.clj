(ns datops-backend.handler
  (:require
   [clojure.string :as str]
   [clojure.data.json :as json]
   [java-time :as jt]
   [compojure.core :refer :all]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [taoensso.timbre :as timbre :refer [info warn error]]
   [taoensso.timbre.appenders.core :as appenders]
   [clojure.java.jdbc :as db]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:import
   [java.sql Timestamp]
   [java.time ZoneId]))

(timbre/merge-config!
 {:appenders {:println {:enabled? false}
              :spit (appenders/spit-appender {:fname "opts-backend.log"})}})

(def db (-> "resources/db.edn" slurp read-string))

(defn get-count [] (-> (db/query db "SELECT COUNT(*) FROM live") first vals first))

(defn get-quotes [symbs]
  (try
    (db/query
     db
     (str "SELECT * FROM live_quote WHERE "
          (str/join
           " OR "
           (map (fn [symb] (str "symbol=" (pr-str symb))) symbs))))
    (catch Exception e [])))

(defn get-fundamentals [symbs]
  (try
    (db/query
     db
     (str "SELECT * FROM fundamentals WHERE "
          (str/join
           " OR "
           (map (fn [symb] (str "symbol=" (pr-str symb))) symbs))))
    (catch Exception e [])))

(defn get-catalysts
  [symbs]
  (let [data (map (juxt :symbol (comp json/read-str :data)) (get-fundamentals symbs))]
    (->> data
         (map (fn [[ticker d]]
                (let [events (get d "calendarEvents")]
                  [ticker
                   {"earnings" (get-in events ["earnings" "earningsDate"])
                    "dividends" (get events "dividendDate")}])))
         (into {}))))

(def order-aliases
  {"e_desc" "expiration desc"
   "e_asc" "expiration asc"
   "iv_desc" "impliedvolatility desc"
   "iv_asc" "impliedvolatility asc"
   "lp_desc" "lastprice desc"
   "lp_asc" "lastprice asc"
   "md_desc" "regularmarketprice desc"
   "md_asc" "regularmarketprice asc"
   "mdh_desc" "regularmarketdayhigh desc"
   "mdh_asc" "regularmarketdayhigh asc"
   "mdl_desc" "regularmarketdaylow desc"
   "mdl_asc" "regularmarketdaylow asc"})

(defn parse-tickers
  [tickers exclude]
  (let [candidates (-> tickers
                       str/upper-case
                       (str/split #"[ ,]+"))]
    (->> candidates
         (filter #(re-matches #"[A-Z]+" %))
         (map
          (fn [t]
            (if exclude
              (str "(live.symbol <> \"" t "\")")
              (str "(live.symbol = \"" t "\")"))))
         (str/join (if exclude " AND " " OR ")))))

;; (parse-tickers "AAB,BAD TSLA aapl C132" true)
;; => "(symbol <> AAB) AND (symbol <> BAD) AND (symbol <> TSLA) AND (symbol <> AAPL)"
;; (parse-tickers "AAB,BAD TSLA aapl C132" false)
;; => "(symbol = AAB) OR (symbol = BAD) OR (symbol = TSLA) OR (symbol = AAPL)"

(defn sanitize-query
  [{:keys [limit
           min-diff max-diff
           min-ask-bid max-ask-bid
           min-exp max-exp
           min-price max-price
           min-sto max-sto
           min-pso max-pso
           min-yield max-yield
           min-myield max-myield
           min-cap max-cap] :as req}]
  (-> req
      (assoc :limit (try (Integer/parseInt limit) (catch Exception e 50)))
      (assoc :min-diff (try (Integer/parseInt min-diff) (catch Exception e nil)))
      (assoc :max-diff (try (Integer/parseInt max-diff) (catch Exception e nil)))
      (assoc :min-ask-bid (try (Double/parseDouble min-ask-bid) (catch Exception e nil)))
      (assoc :max-ask-bid (try (Double/parseDouble max-ask-bid) (catch Exception e nil)))
      (assoc :min-exp (try (Integer/parseInt min-exp) (catch Exception e nil)))
      (assoc :max-exp (try (Integer/parseInt max-exp) (catch Exception e nil)))
      (assoc :min-price (try (Double/parseDouble min-price) (catch Exception e nil)))
      (assoc :max-price (try (Double/parseDouble max-price) (catch Exception e nil)))
      (assoc :min-sto (try (Double/parseDouble min-sto) (catch Exception e nil)))
      (assoc :max-sto (try (Double/parseDouble max-sto) (catch Exception e nil)))
      (assoc :min-pso (try (Double/parseDouble min-pso) (catch Exception e nil)))
      (assoc :max-pso (try (Double/parseDouble max-pso) (catch Exception e nil)))
      (assoc :min-yield (try (Double/parseDouble min-yield) (catch Exception e nil)))
      (assoc :max-yield (try (Double/parseDouble max-yield) (catch Exception e nil)))
      (assoc :min-myield (try (Double/parseDouble min-myield) (catch Exception e nil)))
      (assoc :max-myield (try (Double/parseDouble max-myield) (catch Exception e nil)))
      (assoc :min-cap (try
                        (long (* 1E9 (Double/parseDouble min-cap)))
                        (catch Exception e nil)))
      (assoc :max-cap (try
                        (long (* 1E9 (Double/parseDouble max-cap)))
                        (catch Exception e nil)))))

(defn run-query [{:keys [tickers exclude
                         min-diff max-diff itm otm
                         min-ask-bid max-ask-bid
                         min-exp max-exp
                         min-price max-price
                         calls puts
                         stock etf
                         min-sto max-sto
                         min-pso max-pso
                         min-yield max-yield
                         min-myield max-myield
                         min-cap max-cap
                         order-by limit active]
                  :as req}]
  (let [order-column (get order-aliases order-by "impliedvolatility desc")
        tickers (if tickers (parse-tickers tickers exclude) "")
        cur-time (int (/ (System/currentTimeMillis) 1000))

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
         (str " expiration > " (if min-exp (+ cur-time (* min-exp 3600 24)) cur-time))
         ;; Max expiration date
         (when max-exp (str " AND expiration <= " (+ cur-time (* max-exp 3600 24))))

         ;; Ticker selection
         (when (> (count tickers) 0)
           (if (or (str/includes? tickers "OR")
                   (str/includes? tickers "AND"))
             (str " AND (" tickers ")")
             (str " AND " tickers)))

         ;; Stock to strike diff
         (when min-diff
           (str " AND ((strike <= " (float (- 1 (/ min-diff 100)))
                " * regularmarketprice)"
                " OR (strike >= " (float (+ 1 (/ min-diff 100)))
                " * regularmarketprice))"))
         (when max-diff
           (str " AND strike >= " (float (- 1 (/ max-diff 100)))
                " * regularmarketprice"
                " AND strike <= " (float (+ 1 (/ max-diff 100)))
                " * regularmarketprice"))
         (when (not itm)
           (str " AND (((strike >= regularmarketprice) AND (opttype = \"C\"))"
                " OR ((strike <= regularmarketprice) AND (opttype = \"P\")))"))
         (when (not otm)
           (str " AND (((strike <= regularmarketprice) AND (opttype = \"C\"))"
                " OR ((strike >= regularmarketprice) AND (opttype = \"P\")))"))

         ;; Ask bid spread
         (when min-ask-bid
           (str " AND ask - bid >=" min-ask-bid))
         (when max-ask-bid
           (str " AND ask - bid <=" max-ask-bid))

         ;; Premium (put a minimum price by default not to show options with no premium
         (when max-price
           (str " AND ((ask <> 0 AND ask <= " max-price ") OR (ask = 0 AND lastprice <= " max-price "))"))
         " AND ((ask <> 0 AND ask >= " (if min-price min-price 0.001)
         ;; Here we do this because sometimes, yahoo resets all ask prices to 0
         ;; this allows us to check last price if it is the case
         ") OR (ask = 0 AND lastprice >= " (if min-price min-price 0.001) "))"

         ;; Opt-type
         (when (not puts) " AND opttype <> \"P\"")
         (when (not calls) " AND opttype <> \"C\"")

         ;; Stock type
         (cond (and etf (not stock)) " AND quotetype = \"ETF\""
               (and stock (not etf)) " AND quotetype <> \"ETF\""
               :else "")

         ;; Stock/Option price ratio
         (when min-sto
           (str " AND (100*ask)/regularmarketprice >= " min-sto))
         (when max-sto
           (str " AND (100*ask)/regularmarketprice <= " max-sto))

         ;; Premium/Strike ratio
         (when min-pso
           (str " AND ((ask <> 0 AND ask/strike >= " min-pso
                ") OR (ask = 0 AND lastprice/strike >= " min-pso "))"))
         (when max-pso
           (str " AND ((ask <> 0 AND ask/strike <= " max-pso
                ") OR (ask = 0 AND lastprice/strike <= " max-pso "))"))

         ;; Yield
         (when min-yield
           (str " AND yield >=" min-yield))
         (when max-yield
           (str " AND yield <=" min-yield))

         ;; Monthly yield
         (when min-myield
           (str " AND monthlyyield >=" min-myield))
         (when max-myield
           (str " AND monthlyyield <=" max-myield))

         ;; Market cap
         (when min-cap
           (str " AND JSON_VALUE(live_quote.data, '$.marketCap') >= " min-cap))
         (when max-cap
           (str " AND JSON_VALUE(live_quote.data, '$.marketCap') <= " max-cap))

         (when active
           " AND ask > 0 AND bid > 0 AND volume > 0 AND openinterest > 0")
         )

        ;; Last traded in the last week (old "active" flag, by default for the moment)
        query (str query (str " AND lasttradedate > "
                              (- (int (/ (System/currentTimeMillis) 1000))
                                 (* 24 3600 21))
                              " AND lastcrawl > "
                              (- (int (/ (System/currentTimeMillis) 1000))
                                 (* 24 3600 3))))
        query (str query " ORDER BY " order-column)
        query (str query " LIMIT " (min (or limit 50) 50))]
    (info "--- Received:" req "→" query)
    (try
      (db/query db query)
      (catch Exception _ {:error "Error in query."}))))

(defn get-if-market
  "Input ts
  Returns ts if it is a valid market time, otherwise the next possible market time"
  []
  (let [d (-> (System/currentTimeMillis)
              (Timestamp.)
              .toLocalDateTime
              (.atZone (ZoneId/systemDefault))
              (jt/with-zone-same-instant "America/New_York")
              jt/as-map)
        {:keys [day-of-week hour-of-day minute-of-hour second-of-day]} d
        mins (+ minute-of-hour (* hour-of-day 60))]
    (cond (= 6 day-of-week)   ;; Saturday
          "CLOSED"
          
          (= 7 day-of-week)   ;; Sunday
          "CLOSED"
          
          (< hour-of-day 4)        ;; Pre-market open (4h)
          "CLOSED"
          
          (>= hour-of-day 20) ;; End market open (20h)
          "CLOSED"

          (and (< hour-of-day 20) (>= hour-of-day 16))
          "POST"

          (and (>= hour-of-day 4)
               (or (< hour-of-day 9)
                   (and (= hour-of-day 9) (< minute-of-hour 30))))
          "PRE"

          (and (<= hour-of-day 16)
               (or (>= hour-of-day 10)
                   (and (= hour-of-day 9) (>= minute-of-hour 30))))
          "OPEN")))


(defroutes app-routes
  (GET "/" [] "Hi :)")
  (GET "/index.html" req (pr-str req))
  (GET "/count" []
       (let [res (get-count)]
         (pr-str {:opts-nb res})))
  (GET "/query" {:keys [params]}
       (let [res (-> params :params read-string sanitize-query run-query)
             symbols (map :symbol res)
             quotes (->> symbols
                         get-quotes
                         (map
                          (fn [{symb :symbol data :data}]
                            [symb (json/read-str data :key-fn keyword)]))
                         (into {}))             
             catalysts (get-catalysts symbols)]
         (pr-str {:options res :quotes quotes :catalysts catalysts})))
  (GET "/ops" req
       (let [res (-> req :body slurp (json/read-str :key-fn keyword) run-query)]
         (json/write-str res)))
  (GET "/market/status" req (pr-str {:status (get-if-market)}))
  (route/not-found "Not Found"))

(def app
  (->
   (handler/site app-routes)
   (wrap-cors :access-control-allow-origin [#".*"]
              :access-control-allow-methods [:get :put :post :delete])))

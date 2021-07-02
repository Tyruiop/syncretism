(ns live-opts.communication
  (:require
   [goog.dom :as gdom]
   [cljs-http.client :as http]
   [cljs.reader :refer [read-string]]
   [cljs.core.async :as async :refer [<! go]]
   
   [live-opts.state :refer [state]]))

(def srv-addr "https://api.syncretism.io")

(defn get-market-status
  []
  (go
    (let [resp
          (<! (http/get (str srv-addr "/market/status")
                        {:with-credentials? false}))]
      (swap! state #(assoc % :market-status (-> resp :body read-string :status))))))

(defn get-ladder
  [ticker expiration opttype]
  (go
    (let [resp
          (<! (http/get
               (str srv-addr "/ops/ladder/" ticker "/" opttype "/" expiration)
               {:with-credentials? false}))
          data (->> resp
                    :body
                    (.parse js/JSON))
          clj-data (js->clj data :keywordize-keys true)
          ladder (->> clj-data
                      (map
                       (fn [{:keys [contractsymbol] :as d}]
                         [contractsymbol d]))
                      (into {}))]
      (swap! state #(assoc-in % [:ladders [ticker expiration opttype]] ladder)))))

(def order-aliases
  {"e_desc" {:sort-key 4 :reversed true}
   "e_asc" {:sort-key 4 :reversed false}
   "iv_desc" {:sort-key 5 :reversed true}
   "iv_asc" {:sort-key 5 :reversed false}
   "lp_desc" {:sort-key 6 :reversed true}
   "lp_asc" {:sort-key 6 :reversed false}
   "mdh_desc" {:sort-key 13 :reversed true}
   "mdh_asc" {:sort-key 13 :reversed false}
   "mdl_desc" {:sort-key 12 :reversed true}
   "mdl_asc" {:sort-key 12 :reversed false}
   "md_desc" {:sort-key 11 :reversed true}
   "md_asc" {:sort-key 11 :reversed false}})

(defn send-query
  [state]
  (swap! state #(assoc % :status :loading))
  (let [;; Ticker selection
        tickers (.-value (gdom/getElement "tickers-value"))
        exclude (.-checked (gdom/getElement "exclude"))

        ;; Stock to strike diff
        min-diff (.-value (gdom/getElement "min-diff-value"))
        max-diff (.-value (gdom/getElement "max-diff-value"))
        otm (.-checked (gdom/getElement "otm"))
        itm (.-checked (gdom/getElement "itm"))

        ;; Bid ask spread
        min-ask-bid (.-value (gdom/getElement "min-ask-bid-value"))
        max-ask-bid (.-value (gdom/getElement "max-ask-bid-value"))
        
        ;; Exp date
        min-exp (.-value (gdom/getElement "min-exp-value"))
        max-exp (.-value (gdom/getElement "max-exp-value"))

        ;; Premium
        max-price (.-value (gdom/getElement "max-price-value"))
        min-price (.-value (gdom/getElement "min-price-value"))

        ;; Opt-type
        puts (.-checked (gdom/getElement "puts"))
        calls (.-checked (gdom/getElement "calls"))

        ;; Security type
        stock (.-checked (gdom/getElement "stock"))
        etf (.-checked (gdom/getElement "etf"))

        ;; Stock/option price ratio
        min-sto (.-value (gdom/getElement "min-sto-value"))
        max-sto (.-value (gdom/getElement "max-sto-value"))

        ;; Yield
        min-yield (.-value (gdom/getElement "min-yield-value"))
        max-yield (.-value (gdom/getElement "max-yield-value"))

        ;; Monthly yield
        min-myield (.-value (gdom/getElement "min-myield-value"))
        max-myield (.-value (gdom/getElement "max-myield-value"))        

        ;; Greeks
        min-delta (.-value (gdom/getElement "min-delta-value"))
        max-delta (.-value (gdom/getElement "max-delta-value"))
        min-gamma (.-value (gdom/getElement "min-gamma-value"))
        max-gamma (.-value (gdom/getElement "max-gamma-value"))
        min-theta (.-value (gdom/getElement "min-theta-value"))
        max-theta (.-value (gdom/getElement "max-theta-value"))
        min-vega (.-value (gdom/getElement "min-vega-value"))
        max-vega (.-value (gdom/getElement "max-vega-value"))
        
        ;; Market capitalization
        min-cap (.-value (gdom/getElement "min-cap-value"))
        max-cap (.-value (gdom/getElement "max-cap-value"))

        ;; Extra
        order-by (.-value (gdom/getElement "order-by-value"))
        limit (.-value (gdom/getElement "limit-value"))
        active (.-checked (gdom/getElement "active"))]
    (-> js/window
        .-history
        (.replaceState
         nil nil
         (str
          ;; Ticker selection
          "index.html?tickers=" (js/encodeURIComponent tickers)
          "&exclude=" (js/encodeURIComponent exclude)

          ;; Stock to strike diff
          "&min-diff=" (js/encodeURIComponent min-diff)
          "&max-diff=" (js/encodeURIComponent max-diff)
          "&itm=" (js/encodeURIComponent itm)
          "&otm=" (js/encodeURIComponent otm)
          
          ;; Ask bid spread
          "&min-ask-bid=" (js/encodeURIComponent min-ask-bid)
          "&max-ask-bid=" (js/encodeURIComponent max-ask-bid)

          ;; Expiration
          "&min-exp=" (js/encodeURIComponent min-exp)
          "&max-exp=" (js/encodeURIComponent max-exp)

          ;; Premium
          "&max-price=" (js/encodeURIComponent max-price)
          "&min-price=" (js/encodeURIComponent min-price)

          ;; Opt-type
          "&calls=" (js/encodeURIComponent calls)
          "&puts=" (js/encodeURIComponent puts)

          ;; Security type
          "&etf=" (js/encodeURIComponent etf)
          "&stock=" (js/encodeURIComponent stock)

          ;; Stock/option price ratio
          "&min-sto=" (js/encodeURIComponent min-sto)
          "&max-sto=" (js/encodeURIComponent max-sto)

          ;; Yield
          "&min-yield=" (js/encodeURIComponent min-yield)
          "&max-yield=" (js/encodeURIComponent max-yield)

          ;; Monthly yield
          "&min-myield=" (js/encodeURIComponent min-myield)
          "&max-myield=" (js/encodeURIComponent max-myield)

          ;; Greeks
          "&min-delta=" (js/encodeURIComponent min-delta)
          "&max-delta=" (js/encodeURIComponent max-delta)
          "&min-gamma=" (js/encodeURIComponent min-gamma)
          "&max-gamma=" (js/encodeURIComponent max-gamma)
          "&min-theta=" (js/encodeURIComponent min-theta)
          "&max-theta=" (js/encodeURIComponent max-theta)
          "&min-vega=" (js/encodeURIComponent min-vega)
          "&max-vega=" (js/encodeURIComponent max-vega)

          ;; Market cap
          "&min-cap=" (js/encodeURIComponent min-cap)
          "&max-cap=" (js/encodeURIComponent max-cap)

          ;; Extra
          "&order-by=" (js/encodeURIComponent order-by)
          "&limit=" (js/encodeURIComponent limit)
          "&active=" (js/encodeURIComponent active))))
    (go
      (let [resp
            (<! (http/get
                 (str srv-addr "/query")
                 {:with-credentials? false
                  :query-params
                  {:params
                   (pr-str
                    {:tickers tickers :exclude exclude
                     :min-diff min-diff :max-diff max-diff :otm otm :itm itm
                     :min-ask-bid min-ask-bid :max-ask-bid max-ask-bid
                     :min-exp min-exp :max-exp max-exp
                     :min-price min-price :max-price max-price
                     :puts puts :calls calls
                     :stock stock :etf etf
                     :min-sto min-sto :max-sto max-sto
                     :min-yield min-yield :max-yield max-yield
                     :min-myield min-myield :max-myield max-myield
                     :min-delta min-delta :max-delta max-delta
                     :min-gamma min-gamma :max-gamma max-gamma
                     :min-theta min-theta :max-theta max-theta
                     :min-vega min-vega :max-vega max-vega
                     :min-cap min-cap :max-cap max-cap
                     :order-by order-by :limit limit :active active
                     })}}))
            {:keys [quotes options catalysts]} (-> resp :body read-string)
            quotes (if (contains? quotes :error)
                     []
                     quotes)]
        (swap! state #(-> %
                          (assoc :cur-quotes quotes)
                          (assoc :cur-results options)
                          (assoc :cur-catalysts catalysts)
                          (assoc :cur-sort (get order-aliases order-by))
                          (assoc :status :results)))))))

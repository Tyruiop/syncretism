(ns live-opts.core
  (:require
   [goog.string :as gstring]
   [goog.string.format]
   [clojure.string :as str]
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [cljs-http.client :as http]
   [cljs.reader :refer [read-string]]
   [cljs.core.async :as async :refer [<! go]]))

;; state and minor-state
;; ---------------------
;; minor-state represent things that might need to be refreshed
;; regularly without affecting UX (i.e. not triggering a re-draw
;; of user's interactive area).
(def state
  (r/atom
   {:cur-results nil
    :cur-sort nil
    :cur-quotes nil
    :cur-visible-quotes #{}
    :status :loading}))
(defn reset-state []
  (swap! state #(-> %
                    (assoc :cur-results nil)
                    (assoc :cur-quotes nil)
                    (assoc :cur-visible-quotes #{})
                    (assoc :cur-sort nil))))
(defn print-opts-ex [] (-> @state :cur-results first println))
(defn print-quotes-ex [] (-> @state :cur-quotes first println))

(def srv-addr "https://api.syncretism.io")

(defn get-market-status
  []
  (go
    (let [resp
          (<! (http/get (str srv-addr "/market/status")
                        {:with-credentials? false}))]
      (swap! state #(assoc % :market-status (-> resp :body read-string :status))))))

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

(def columns-w-names
  [[:contractsymbol "Contract Symbol" "CS"]
   [:symbol "Symbol" "S"]
   [:opttype "Type" "T"]
   [:strike "Strike" "Str"]
   [:expiration "Expiration" "Exp"]
   ;;[:lasttradedate "Last Trade Date" "LTD"]
   [:impliedvolatility "Implied Volatility" "IV"]
   [:lastprice "Last Price" "LP"]
   [:bid "Bid" "B"]
   [:ask "Ask" "A"]
   [:volume "Volume" "V"]
   [:openinterest "Open Interest" "OI"]
   ;;[:inthemoney "In the Money" "ItM"]
   ;;[:pchange "Price Change" "PC"]
   [:regularmarketprice "Stock Market Price" "SMP"]
   [:regularmarketdaylow "Stock Market Day Low" "SMDL"]
   [:regularmarketdayhigh "Stock Market Day High" "SMDH"]
   ;;[:quotetype "Quote Type" "QT"]
   [:lastcrawl "Last Updated" "LU"]
   ])

(def nb-columns (count columns-w-names))

(defn send-query
  [state]
  (swap! state #(assoc % :status :loading))
  (let [;; Ticker selection
        tickers (.-value (gdom/getElement "tickers-value"))
        exclude (.-checked (gdom/getElement "exclude"))

        ;; Stock to strike diff
        min-diff (.-value (gdom/getElement "min-diff-value"))
        max-diff (.-value (gdom/getElement "max-diff-value"))

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

        ;; Premium/strike ratio
        min-pso (.-value (gdom/getElement "min-pso-value"))
        max-pso (.-value (gdom/getElement "max-pso-value"))        

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

          ;; Premium/strike ratio
          "&min-pso=" (js/encodeURIComponent min-pso)
          "&max-pso=" (js/encodeURIComponent max-pso)

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
                     :min-diff min-diff :max-diff max-diff
                     :min-ask-bid min-ask-bid :max-ask-bid max-ask-bid
                     :min-exp min-exp :max-exp max-exp
                     :min-price min-price :max-price max-price
                     :puts puts :calls calls
                     :stock stock :etf etf
                     :min-sto min-sto :max-sto max-sto
                     :min-pso min-pso :max-pso max-pso
                     :min-cap min-cap :max-cap max-cap
                     :order-by order-by :limit limit :active active
                     })}}))
            {:keys [quotes options]} (-> resp :body read-string)]
        (swap! state #(-> %
                          (assoc :cur-quotes quotes)
                          (assoc :cur-results options)
                          (assoc :cur-sort (get order-aliases order-by))
                          (assoc :status :results)))))))

(defn cur-ny-time
  []
  (-> (js/Date.)
      (.toLocaleString "en-US" (clj->js {:timeZone "America/New_York"}))
      js/Date.parse
      (/ 1000)
      int))

(defn cur-gmt-time
  []
  (-> (js/Date.)
      (.toLocaleString "en-US" (clj->js {:timeZone "GMT"}))
      js/Date.parse
      (/ 1000)
      int))

(defn cur-local-time
  []
  (-> (js/Date.)
      (.toLocaleString "en-US")
      js/Date.parse
      (/ 1000)
      int))

(def offset (- (cur-gmt-time) (cur-ny-time)))
(def offset-exp (- (cur-gmt-time) (cur-local-time)))

(defn from-ts
  [ts]
  (-> ts
      (* 1000)
      (js/Date.)
      (.toLocaleString "en-US")
      ))

(defn s-to-h-min
  "Takes seconds and convert them to h:min:s"
  [s]
  (let [hs (.floor js/Math (/ s 3600))
        mins (.floor js/Math (/ (mod s 3600) 60))]
    (cond (> hs 0) (str hs "h" (if (< mins 10) (str "0" mins) mins) "min")
          (> mins 0) (str mins "min")
          :else "< 1 min")))

(defn show-quote-data
  [contractsymbol
   {:keys [symbol displayName longName quoteType
           ask askSize bid bidSize averageDailyVolume10Day averageDailyVolume3Month
           bookValue dividendDate earningsTimestamp epsCurrentYear exchangeDataDelayedBy
           fiftyDayAverage fiftyDayAverageChange fiftyDayAverageChangePercent
           fiftyTwoWeekHigh fiftyTwoWeekHighChangefiftyTwoWeekHighChangePercent
           fiftyTwoWeekLow fiftyTwoWeekRange
           forwardPE
           market marketCap sharesOutstanding
           messageBoardId
           postMarketChange postMarketChangePercent postMarketPrice
           priceEpsCurrentYear priceHint priceToBook
           regularMarketChange regularMarketChangePercent regularMarketDayRange
           regularMarketOpen regularMarketPreviousClose regularMarketPrice
           regularMarketVolume
           tradeable
           trailingAnnualDividendRate trailingAnnualDividendYield
           trailingPE]
    :as data}]
  [:div.quotedata
   [:div.header
    [:h3 (str symbol " - " (or longName displayName) " (" quoteType ")")]
    [:p
     [:a {:href (str "https://finance.yahoo.com/quote/" symbol)
          :target "_blank"}
      "yahoo stock overview"]]
    [:p
     [:a {:href (str "https://finance.yahoo.com/quote/" contractsymbol)
          :target "_blank"}
      "yahoo option overview"]]]
   [:div.columns
    [:div.column
     [:div.entry [:p.key "Prev. Close"] [:p.value regularMarketPreviousClose]]
     [:div.entry [:p.key "Open"] [:p.value regularMarketOpen]]
     [:div.entry [:p.key "Ask"] [:p.value ask " × " askSize]]
     [:div.entry [:p.key "Bid"] [:p.value bid " × " bidSize]]
     [:div.entry [:p.key "Day's Range"] [:p.value regularMarketDayRange]]
     [:div.entry [:p.key "52 Week's Range"] [:p.value fiftyTwoWeekRange]]]
    [:div.column
     (when marketCap
       [:div.entry
        [:p.key "Market Cap."]
        [:p.value (str "$" (when marketCap
                             (.toLocaleString marketCap)))]])
     [:div.entry
      [:p.key "Outsanding Shares"]
      [:p.value (when sharesOutstanding
                  (.toLocaleString sharesOutstanding))]]
     [:div.entry [:p.key "P/E"] [:p.value trailingPE]]
     [:div.entry
      [:p.key "Earnings"]
      [:p.value
       (when earningsTimestamp
         (from-ts earningsTimestamp))]]
     [:div.entry [:p.key "Divident Rate"] [:p.value trailingAnnualDividendRate]]
     [:div.entry [:p.key "Book Value"] [:p.value bookValue]]]]])

(defn draw-contract-symbol
  [contractsymbol]
  [:<>
   [:a
    {:href "#"
     :on-click
     (fn []
       (if (contains? (:cur-visible-quotes @state) contractsymbol)
         (swap! state #(update % :cur-visible-quotes disj contractsymbol))
         (swap! state #(update % :cur-visible-quotes conj contractsymbol))))}
    contractsymbol]
   [:div.marker
    {:class [(when (contains? (:cur-visible-quotes @state) contractsymbol) "rev")]
     :on-click
     (fn []
       (if (contains? (:cur-visible-quotes @state) contractsymbol)
         (swap! state #(update % :cur-visible-quotes disj contractsymbol))
         (swap! state #(update % :cur-visible-quotes conj contractsymbol))))}
    " "]])

(defn draw-symbol
  [symbol]
  (let [{:keys [dividendDate earningsTimestamp]} (get quotes symbol)
        cur-time (cur-ny-time)]
    [:<>
     [:span.symbol symbol]
     (when (and dividendDate (> dividendDate cur-time))
       [:span.dividend "D" [:span.hover (from-ts dividendDate)]])
     (when (and earningsTimestamp (> earningsTimestamp cur-time))
       [:span.dividend "E" [:span.hover (from-ts earningsTimestamp)]])]))

(defn landing-loading
  []
  [:div {:class ["loading"]}
   [:p "Loading..."]])

(defn landing-results
  [state]
  (let [cur-time (- (cur-local-time) offset)
        {:keys [sort-key reversed]} (:cur-sort @state)]
    [:div.content-wrapper
     [:table.results
      [:thead
       [:tr
        (doall
         (keep-indexed
          (fn [idx [id c-name short-name]]
            [:th
             {:key id :class [(name id)] :title c-name}
             [:p
              {:on-click
               (fn [_]
                 (swap!
                  state
                  #(update % :cur-sort
                           (fn [{:keys [sort-key] :as old}]
                             (if (= sort-key idx)
                               (update old :reversed not)
                               {:sort-key idx :reversed false})))))}
              [:strong short-name
               [:span.order
                (when (= sort-key idx)
                  (if reversed
                    " ∨"
                    " ∧"))]]]])
          columns-w-names))]]
      [:tbody
       (let [quotes (:cur-quotes @state)]
         (doall
          (map
           (fn [{:keys [contractsymbol] :as entry}]
             [:<> {:key (str "d-" contractsymbol)}
              [:tr.d {:class ["result"]}
               (doall
                (map
                 (fn [[id _]]
                   (let [v (get entry id)]
                     [:td {:key (str contractsymbol "-" (name id))
                           :class [(name id)
                                   (cond
                                     (= id :inthemoney) (if v "true" "false")
                                     (= id :opttype) v)]
                           :title (when (number? v) v)}
                      [:div
                       (cond (= id :lastcrawl) (s-to-h-min (- cur-time v))

                             (= id :impliedvolatility) (gstring/format "%.2f" v)
                             
                             (or (= id :expiration) (= id :lasttradedate))
                             (-> (from-ts (+ (or v 0) offset-exp)) (str/split #",") first)

                             (or (= id :ask) (= id :bid) (= id :lastprice)
                                 (= id :regularmarketprice)
                                 (= id :regularmarketdaylow)
                                 (= id :regularmarketdayhigh))
                             (if (number? v) [:<> "$" (.toFixed v 2)] v)

                             (= id :contractsymbol)
                             (draw-contract-symbol contractsymbol)

                             (= id :symbol)
                             (draw-symbol v)
                             
                             :else (str v))]]))
                 columns-w-names))]
              (when (contains? (:cur-visible-quotes @state) contractsymbol)
                [:tr.q {:key (str "q-" contractsymbol)}
                 [:td {:colspan nb-columns}
                  (show-quote-data contractsymbol (get quotes (:symbol entry)))]])])
           (cond (and sort-key reversed)
                 (reverse (sort-by (first (nth columns-w-names sort-key)) (:cur-results @state)))
                 sort-key (sort-by (first (nth columns-w-names sort-key)) (:cur-results @state))
                 :else (:cur-results @state)))))]]]))

(defn landing [state]
  (r/create-class
   {:reagent-render
    (fn [state]
      (case (:status @state)
        :loading (landing-loading)
        :results (landing-results state)))

    :display-name "UI"}))

(defn status-bar [state]
  (r/create-class
   {:reagent-render
    (fn [state]
      (let [m-status (:market-status @state)]
        [:p {:class ["m-status" m-status]} (str "Market " m-status)]))
    :display-name "Status"}))

(defn init []
  (get-market-status)
  (js/setInterval get-market-status 120000)
  (doseq [el (.getElementsByTagName js/document "input")]
    (.addEventListener
     el "keydown"
     (fn [ev] (when (= (.-keyCode ev) 13) (send-query state)))))
  (doseq [el (.getElementsByTagName js/document "button")]
    (.addEventListener el "click" (fn [] (send-query state))))
  (let [q-string (.. js/window -location -search)
        url-params (js->clj (new js/URLSearchParams q-string))
        order-by (.get url-params "order-by")
        limit  (.get url-params "limit")
        active (.get url-params "active")]
    (when order-by
      (set! (.-value (gdom/getElement "order-by-value")) order-by))
    (when limit
      (set! (.-value (gdom/getElement "limit-value")) limit))
    (when active
      (set!
       (.-checked (gdom/getElement "active"))
       (or (= "true" active) (= "on" active))))

    ;; stock to strike diff
    (let [min-diff (.get url-params "min-diff")
          max-diff (.get url-params "max-diff")]
      (when min-diff
        (set! (.-value (gdom/getElement "min-diff-value")) min-diff))
      (when max-diff
        (set! (.-value (gdom/getElement "max-diff-value")) max-diff)))

    ;; bid ask spread
    (let [min-ask-bid (.get url-params "min-ask-bid")
          max-ask-bid (.get url-params "max-ask-bid")]
      (when min-ask-bid
        (set! (.-value (gdom/getElement "min-ask-bid-value")) min-ask-bid))
      (when max-ask-bid
        (set! (.-value (gdom/getElement "max-ask-bid-value")) max-ask-bid)))

    ;; Days to expiration date
    (let [min-exp (.get url-params "min-exp")
          max-exp (.get url-params "max-exp")]
      (when min-exp
        (set! (.-value (gdom/getElement "min-exp-value")) min-exp))
      (when max-exp
        (set! (.-value (gdom/getElement "max-exp-value")) max-exp)))

    ;; Premium
    (let [min-price (.get url-params "min-price")
          max-price (.get url-params "max-price")]
      (when max-price
        (set! (.-value (gdom/getElement "max-price-value")) max-price))
      (when min-price
        (set! (.-value (gdom/getElement "min-price-value")) min-price)))

    ;; Opt type
    (let [calls (.get url-params "calls")
          puts (.get url-params "puts")]
      (when calls
        (set!
         (.-checked (gdom/getElement "calls"))
         (or (= "true" calls) (= "on" calls))))
      (when puts
        (set!
         (.-checked (gdom/getElement "puts"))
         (or (= "true" puts) (= "on" puts)))))

    ;; Security type
    (let [stock (.get url-params "stock")
          etf (.get url-params "etf")]
      (when stock
        (set!
         (.-checked (gdom/getElement "stock"))
         (or (= "true" stock) (= "on" stock))))
      (when etf
        (set!
         (.-checked (gdom/getElement "etf"))
         (or (= "true" etf) (= "on" etf)))))

    ;; Stock/Option price ratio
    (let [min-sto (.get url-params "min-sto")
          max-sto (.get url-params "max-sto")]
      (when min-sto
        (set! (.-value (gdom/getElement "min-sto-value")) min-sto))
      (when max-sto
        (set! (.-value (gdom/getElement "max-sto-value")) max-sto)))

    ;; Premium/Strike
    (let [min-pso (.get url-params "min-pso")
          max-pso (.get url-params "max-pso")]
      (when min-pso
        (set! (.-value (gdom/getElement "min-pso-value")) min-pso))
      (when max-pso
        (set! (.-value (gdom/getElement "max-pso-value")) max-pso)))

    ;; Market cap
    (let [min-cap (.get url-params "min-cap")
          max-cap (.get url-params "max-cap")]
      (when min-cap
        (set! (.-value (gdom/getElement "min-cap-value")) min-cap))
      (when max-cap
        (set! (.-value (gdom/getElement "max-cap-value")) max-cap)))

    ;; Ticker selection
    (let [tickers (.get url-params "tickers")
          exclude (.get url-params "exclude")]
      (when tickers
        (set! (.-value (gdom/getElement "tickers-value")) tickers))
      (when exclude
        (set!
         (.-checked (gdom/getElement "exclude"))
         (or (= "true" exclude) (= "on" exclude)))))
    
    ;; Always send query when loading, even if with default values
    (send-query state))
  (rdom/render [(fn []
                  [:<>
                   [landing state]])]
               (gdom/getElement "app"))
  (rdom/render [status-bar state] (gdom/getElement "status")))

(defn ^:after-load on-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  nil)

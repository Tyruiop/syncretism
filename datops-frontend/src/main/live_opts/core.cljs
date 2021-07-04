(ns live-opts.core
  (:require
   [goog.string :as gstring]
   [goog.string.format]
   [clojure.string :as str]
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [reagent.core :as r]

   [live-opts.state :refer [state]]
   [live-opts.communication :refer [get-market-status get-ladder send-query]]))

(def columns-w-names
  [[:contractsymbol "Contract Symbol" "CS"]
   [:symbol "Symbol" "S"]
   [:opttype "Type" "T"]
   [:strike "Strike" "Str"]
   [:expiration "Expiration" "Exp"]
   ;;[:lasttradedate "Last Trade Date" "LTD"]
   [:impliedvolatility "Implied Volatility" "IV"]
   [:bid "Bid" "B"]
   [:ask "Ask" "A"]
   [:lastprice "Last Price" "LP"]
   [:volume "Volume" "V"]
   [:openinterest "Open Interest" "OI"]
   [:yield "Yield" "Y"]
   [:monthlyyield "Monthly Yield" "MY"]
   ;;[:inthemoney "In the Money" "ItM"]
   ;;[:pchange "Price Change" "PC"]
   [:regularmarketprice "Stock Market Price" "SMP"]
   [:regularmarketdaylow "Stock Market Day Low" "SMDL"]
   [:regularmarketdayhigh "Stock Market Day High" "SMDH"]
   [:delta "Delta" "δ"]
   [:gamma "Gamma" "γ"]
   [:theta "Theta" "θ"]
   [:vega "Vega" "ν"]
   ;;[:quotetype "Quote Type" "QT"]
   [:lastcrawl "Last Updated" "LU"]
   ])

(def nb-columns (count columns-w-names))

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

(defn landing-loading
  []
  [:div {:class ["loading"]}
   [:p "Loading..."]])

(defn in-the-money?
  [strike price opttype]
  (or (and (= opttype "C") (>= price strike))
      (and (= opttype "P") (< price strike))))

(defn draw-table-header
  [state sort-key reversed]
  [:thead
   [:tr
    (doall
     (keep-indexed
      (fn [idx [id c-name short-name]]
        [:th
         {:key id :class [(name id)] :title c-name :scope "col"}
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
      columns-w-names))]])

(defn toggle-set
  [s v]
  (if (contains? s v)
    (disj s v)
    (conj s v)))

(defn draw-contract-symbol
  [state contractsymbol ticker expiration opttype]
  [:<>
   [:div.spread
    [:p
     {:on-click
      (fn []
        (when (nil? (get-in @state [:ladders [ticker expiration opttype]]))
          (get-ladder ticker expiration opttype))
        (swap! state #(update % :spreads toggle-set contractsymbol)))}
     (if (contains? (:spreads @state) contractsymbol)
       "-"
       "+")
     [:span "Check spread value"]]]
   [:span
    {:on-click
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
  [ticker]
  (let [catalysts (get-in @state [:cur-catalysts ticker])
        now (cur-ny-time)
        earnings (-> catalysts (get "earnings") first)
        dividends (get catalysts "dividends")]
    [:div.symb
     [:p ticker]
     (when (> (get earnings "raw") now)
       [:div.catalyst.e [:p "E"] [:div.cat-info (str "earnings: " (get earnings "fmt"))]])
     (when (> (get dividends "raw") now)
       [:div.catalyst.d [:p "D"] [:div.cat-info (str "dividends: " (get dividends "fmt"))]])]))

(defn get-next-data
  [contractsymbol ticker expiration opttype]
  (let [ladder (get-in @state [:ladders [ticker expiration opttype]])]
    (when (not (empty? ladder))
      (let [css (to-array (sort (keys ladder)))
            target-index (+ (.indexOf css contractsymbol)
                            (if (= opttype "C") 1 -1))
            ;; If somehow we are at an extremity of the ladder, just go to next available
            target-index (cond (= (.-length css) target-index) (- (.-length css) 2)
                               (= -1 target-index) 1
                               :else target-index)
            target-cs (nth (js->clj css) target-index)]
        (get-in @state [:ladders [ticker expiration opttype] target-cs])))))

(defn draw-row
  [state cur-time
   {:keys [contractsymbol strike regularmarketprice opttype expiration symbol] :as entry}]
  (let [quotes (:cur-quotes @state)
        next (when (contains? (:spreads @state) contractsymbol)
               (get-next-data contractsymbol symbol expiration opttype))]
    [:<> {:key (str "d-" contractsymbol)}
     [:tr.d {:class ["result"
                     (if (in-the-money? strike regularmarketprice opttype)
                       "itm" "otm")
                     (when next "act-spread")]}
      (doall
       (map
        (fn [[id _]]
          (let [v (get entry id nil)]
            [:td {:key (str contractsymbol "-" (name id))
                  :class [(name id)
                          (cond
                            (= id :inthemoney) (if v "true" "false")
                            (= id :opttype) v)]
                  :title (when (number? v) v)}
             [:div
              (cond (= id :lastcrawl) (s-to-h-min (- cur-time v))

                    (or (= id :impliedvolatility)
                        (= id :yield) (= id :monthlyyield))
                    (if (number? v)
                      (gstring/format "%.2f" v)
                      (str v))

                    (contains? #{:delta :theta :gamma :vega} id)
                    (if (number? v) (gstring/format "%.4f" v) "")
                    
                    (or (= id :expiration) (= id :lasttradedate))
                    (if (number? v)
                      (-> (from-ts (+ (or v 0) offset-exp)) (str/split #",") first)
                      (str v))

                    (or (= id :ask) (= id :bid) (= id :lastprice))
                    (if (number? v) [:<> "$" (.toFixed (if next (- v (get next id)) v) 2)] v)

                    (or (= id :regularmarketprice)
                        (= id :regularmarketdaylow)
                        (= id :regularmarketdayhigh))
                    (if (number? v) [:<> "$" (.toFixed v 2)] v)

                    (= id :contractsymbol)
                    (draw-contract-symbol
                     state contractsymbol symbol expiration opttype)

                    (= id :symbol)
                    (draw-symbol v)

                    (= id :strike)
                    (if next (str v "/" (:strike next)) (str v))

                    (= id :openinterest)
                    (if next (str (min v (get next id))) (str v))
                    
                    :else (str v))]]))
        columns-w-names))]
     (when (contains? (:cur-visible-quotes @state) contractsymbol)
       [:tr.q {:key (str "q-" contractsymbol)}
        [:td {:colSpan nb-columns}
         (show-quote-data contractsymbol (get quotes symbol))]])]))

(defn landing-results
  [state]
  (let [cur-time (- (cur-local-time) offset)
        {:keys [sort-key reversed]} (:cur-sort @state)
        results (:cur-results @state)
        limit
        (let [l (js/parseInt
                 (.-value (gdom/getElement "limit-value")))]
          (if (js/isNaN l) 50 l))]
    [:div.content-wrapper
     [:table.results
      (draw-table-header state sort-key reversed)
      [:tbody
       (doall
        (map
         (partial draw-row state cur-time)
         (cond (and sort-key reversed)
               (reverse (sort-by (first (nth columns-w-names sort-key)) results))
               sort-key (sort-by (first (nth columns-w-names sort-key)) results)
               :else results)))]]
     (when (= (mod (count results) limit) 0)
       [:div.more
        {:on-click (fn [] (send-query state (count results)))}
        [:p "More results"]])]))

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
     (fn [ev] (when (= (.-keyCode ev) 13) (send-query state 0)))))
  (.addEventListener (gdom/getElement "send") "click" (fn [] (send-query state 0)))
  (.addEventListener
   (gdom/getElement "clear") "click"
   (fn []
     (doseq [el (.getElementsByTagName js/document "input")]
       (if (= "checkbox" (.-type el))
         (set! (.-checked el) "")
         (set! (.-value el) "")))))
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
          max-diff (.get url-params "max-diff")
          itm (.get url-params "itm")
          otm (.get url-params "otm")]
      (when min-diff
        (set! (.-value (gdom/getElement "min-diff-value")) min-diff))
      (when max-diff
        (set! (.-value (gdom/getElement "max-diff-value")) max-diff))
      (when itm
        (set!
         (.-checked (gdom/getElement "itm"))
         (or (= "true" itm) (= "on" itm))))
      (when otm
        (set!
         (.-checked (gdom/getElement "otm"))
         (or (= "true" otm) (= "on" otm)))))

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
    
    ;; IV
    (let [min-iv (.get url-params "min-iv")
          max-iv (.get url-params "max-iv")]
      (when min-iv
        (set! (.-value (gdom/getElement "min-iv-value")) min-iv))
      (when max-iv
        (set! (.-value (gdom/getElement "max-iv-value")) max-iv)))

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

    ;; Yield
    (let [min-yield (.get url-params "min-yield")
          max-yield (.get url-params "max-yield")]
      (when min-yield
        (set! (.-value (gdom/getElement "min-yield-value")) min-yield))
      (when max-yield
        (set! (.-value (gdom/getElement "max-yield-value")) max-yield)))

    ;; Monthly yield
    (let [min-myield (.get url-params "min-myield")
          max-myield (.get url-params "max-myield")]
      (when min-myield
        (set! (.-value (gdom/getElement "min-myield-value")) min-myield))
      (when max-myield
        (set! (.-value (gdom/getElement "max-myield-value")) max-myield)))

    ;; greeks
    (let [min-delta (.get url-params "min-delta")
          max-delta (.get url-params "max-delta")
          min-gamma (.get url-params "min-gamma")
          max-gamma (.get url-params "max-gamma")
          min-theta (.get url-params "min-theta")
          max-theta (.get url-params "max-theta")
          min-vega (.get url-params "min-vega")
          max-vega (.get url-params "max-vega")]
      (when min-delta
        (set! (.-value (gdom/getElement "min-delta-value")) min-delta))
      (when max-delta
        (set! (.-value (gdom/getElement "max-delta-value")) max-delta))
      (when min-gamma
        (set! (.-value (gdom/getElement "min-gamma-value")) min-gamma))
      (when max-gamma
        (set! (.-value (gdom/getElement "max-gamma-value")) max-gamma))
      (when min-theta
        (set! (.-value (gdom/getElement "min-theta-value")) min-theta))
      (when max-theta
        (set! (.-value (gdom/getElement "max-theta-value")) max-theta))
      (when min-vega
        (set! (.-value (gdom/getElement "min-vega-value")) min-vega))
      (when max-vega
        (set! (.-value (gdom/getElement "max-vega-value")) max-vega)))

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
    (send-query state 0))
  (rdom/render [(fn []
                  [:<>
                   [landing state]])]
               (gdom/getElement "app"))
  (rdom/render [status-bar state] (gdom/getElement "status")))

(defn ^:after-load on-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  nil)

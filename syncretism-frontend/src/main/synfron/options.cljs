(ns synfron.options
  (:require
   [clojure.string :as str]
   [synfron.state :as state]
   [synfron.filters :refer [trigger-search]]))

(def columns-w-names
  [[:contractSymbol "Contract Symbol" "CS"]
   [:symbol "Symbol" "S"]
   [:optType "Type" "T"]
   [:strike "Strike" "Str"]
   [:expiration "Expiration" "Exp"]
   [:lastTradeDate "Last Trade Date" "LTD"]
   [:impliedVolatility "Implied Volatility" "IV"]
   [:bid "Bid" "B"]
   [:ask "Ask" "A"]
   [:lastPrice "Last Price" "LP"]
   [:volume "Volume" "V"]
   [:openInterest "Open Interest" "OI"]
   [:yield "Yield" "Y"]
   [:monthlyyield "Monthly Yield" "MY"]
   [:inTheMoney "In the Money" "ItM"]
   [:pChange "Price Change" "PC"]
   [:regularMarketPrice "Market Price" "MP"]
   [:regularMarketDayLow "Market Day Low" "MDL"]
   [:regularMarketDayHigh "Market Day High" "MDH"]
   [:delta "Delta" "δ"]
   [:gamma "Gamma" "γ"]
   [:theta "Theta" "θ"]
   [:vega "Vega" "ν"]
   [:quoteType "Quote Type" "QT"]
   [:lastCrawl "Last Updated" "LU"]])

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

(defn opt-sidebar
  []
  (let [activ-cols (get-in @state/app-state [:options :columns])
        sidebar (:sidebar @state/app-state)]
    [:div {:class ["sidebar" (when sidebar "show")]}
     [:div
      {:class ["sidebar-toggle"]
       :on-click state/toggle-sidebar}
      [:p (if sidebar "<" ">")]]
     [:h3 "Columns"]
     [:div.columns 
      (doall
       (map
        (fn [[col-id col-name abbrev]]
          (let [col-c-id (str "col-c-" (name col-id))]
            [:div {:class ["col-choice"]
                   :key col-c-id}
             [:input
              {:type "checkbox" :id col-c-id :default-checked (contains? activ-cols col-id)
               :on-change (fn [ev] (state/toggle-column col-id))}]
             [:label {:for col-c-id} (str col-name " (" abbrev ")")]]))
        columns-w-names))]]))

(defn draw-symbol
  [ticker]
  (let [catalysts (get-in @state/app-state [:options :data :catalysts ticker])
        now (cur-ny-time)
        earnings (-> catalysts (get "earnings") first)
        dividends (get catalysts "dividends")]
    [:div.symb
     [:p ticker]
     (when (> (get earnings "raw") now)
       [:div.catalyst.e [:p "E"] [:div.cat-info (str "earnings: " (get earnings "fmt"))]])
     (when (> (get dividends "raw") now)
       [:div.catalyst.d [:p "D"] [:div.cat-info (str "dividends: " (get dividends "fmt"))]])]))

(defn ladder-next
  [{:keys [contractSymbol symbol expiration optType]}]
  (let [ladder (get-in @state/app-state [:options :ladder [symbol expiration optType]])]
    (when (not (empty? ladder))
      (try
        (let [css (to-array (sort (map name (keys ladder))))
              target-index (+ (.indexOf css contractSymbol)
                              (if (= optType "C") 1 -1))
              ;; If somehow we are at an extremity of the ladder, just go to next available
              target-index (cond (= (.-length css) target-index) (- (.-length css) 2)
                                 (= -1 target-index) 1
                                 :else target-index)
              target-cs (nth (js->clj css) target-index)]
          (get-in
           @state/app-state
           [:options :ladder [symbol expiration optType] (keyword target-cs)]))
        (catch js/Error _ nil)))))

(defn draw-cell
  [next id v]
  (cond
    (= id :symbol)
    (draw-symbol v)
    
    (or (= id :expiration) (= id :lastTradeDate))
    [:p
     (if (number? v)
       (-> (from-ts (+ (or v 0) offset-exp)) (str/split #",") first)
       (str v))]
    
    (or (= id :impliedVolatility)
        (= id :yield) (= id :monthlyyield))
    [:p (if (number? v)
          (.toFixed v 2)
          (str v))]

    (or (= id :ask) (= id :bid) (= id :lastPrice))
    (if (number? v) [:<> "$" (.toFixed (if next (- v (get next id)) v) 2)] v)

    (or (= id :regularMarketPrice)
        (= id :regularMarketDayLow)
        (= id :regularMarketDayHigh))
    [:p (if (number? v) [:<> "$" (.toFixed v 2)] v)]

    (= id :strike)
    [:p (if next (str v "/" (:strike next)) (str v))]

    (= id :openInterest)
    [:p (if next (str (min v (get next id))) (str v))]

    :else [:p (str v)]))

(defn spread-button
  [{ticker :symbol expiration :expiration
    optType :optType contractSymbol :contractSymbol}]
  (when (nil?
         (get-in
          @state/app-state
          [:options :ladder [ticker expiration optType]]))
    (.postMessage
     state/worker
     (clj->js {:message "ladder" :data [ticker expiration optType]})))
  (state/toggle-spread contractSymbol))

(defn row
  [{:keys [contractSymbol inTheMoney] :as data}]
  (let [activ-cols (get-in @state/app-state [:options :columns])
        activ-spread?
        (contains? (get-in @state/app-state [:options :spreads]) contractSymbol)
        tracked?
        (contains? (get-in @state/app-state [:home :tracked-options]) contractSymbol)
        next (when activ-spread? (ladder-next data))]
    [:div {:class ["row" (when inTheMoney "itm")]
           :key (str "row-" contractSymbol)}
     [:div {:class ["cell" "buttons"]}
      [:button
       {:on-click (fn [] (state/toggle-tracked-options contractSymbol data))
        :class [(when tracked? "tracked")]}
       (if tracked? "forget" "follow")]
      [:button
       {:on-click (fn [] (spread-button data)) :class [(when activ-spread? "spread")]}
       (if activ-spread? "close" "spread")]]
     (->> columns-w-names
          (keep
           (fn [[col-id _ _]]
             (when (contains? activ-cols col-id)
               [:div {:class ["cell"]
                      :key (str (name col-id) "-" contractSymbol)}
                (let [v (get data col-id)]
                  (draw-cell next col-id v))])))
          doall)]))

(defn render
  []
  (let [activ-cols (get-in @state/app-state [:options :columns])]
    [:<>
     (opt-sidebar)
     [:div {:class ["options"]}
      [:div {:class ["row" "header"]}
       [:div {:class ["cell"]}]
       (->> columns-w-names
            (keep
             (fn [[col-id descr abbrev]]
               (when (contains? activ-cols col-id)
                 [:div {:class ["cell"]
                        :key (str (name col-id) "-header")}
                  [:p abbrev [:span descr]]]))))]
      (->> (get-in @state/app-state [:options :data :options])
           (map row)
           doall)]
     [:div {:class ["options-footer"]}
      [:button
       {:on-click
        (fn []
          (-> (get-in @state/app-state [:options :data :options])
              count
              trigger-search))}
       "See more options"]]]))

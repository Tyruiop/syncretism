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

(defn draw-cell
  [id v]
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

    (or (= id :regularMarketPrice)
        (= id :regularMarketDayLow)
        (= id :regularMarketDayHigh))
    [:p (if (number? v) [:<> "$" (.toFixed v 2)] v)]

    :else [:p (str v)]))

(defn row
  [{:keys [contractSymbol] :as data}]
  (let [activ-cols (get-in @state/app-state [:options :columns])]
    [:div {:class ["row"]
           :key (str "row-" contractSymbol)}
     [:div {:class ["cell"]}
      [:button "track"]
      [:button "spread"]]
     (->> columns-w-names
          (keep
           (fn [[col-id _ _]]
             (when (contains? activ-cols col-id)
               [:div {:class ["cell"]
                      :key (str (name col-id) "-" contractSymbol)}
                (let [v (get data col-id)]
                  (draw-cell col-id v))])))
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

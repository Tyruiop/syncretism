(ns synfron.options
  (:require [synfron.state :as state]))

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
   [:inthemoney "In the Money" "ItM"]
   [:pchange "Price Change" "PC"]
   [:regularMarketPrice "Market Price" "MP"]
   [:regularMarketDayLow "Market Day Low" "MDL"]
   [:regularMarketDayHigh "Market Day High" "MDH"]
   [:delta "Delta" "δ"]
   [:gamma "Gamma" "γ"]
   [:theta "Theta" "θ"]
   [:vega "Vega" "ν"]
   [:quoteType "Quote Type" "QT"]
   [:lastCrawl "Last Updated" "LU"]])

(defn opt-sidebar
  []
  (let [activ-cols (get-in @state/app-state [:options :columns])
        sidebar (get-in @state/app-state [:options :sidebar])]
    [:div {:class ["opt-sidebar" (when sidebar "show")]}
     [:div
      {:class ["opt-sidebar-toggle"]
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

(defn row
  [{:keys [contractSymbol] :as data}]
  (let [activ-cols (get-in @state/app-state [:options :columns])]
    [:div {:class ["row"]
           :key (str "row-" contractSymbol)}
     (->> columns-w-names
          (keep
           (fn [[col-id _ _]]
             (when (contains? activ-cols col-id)
               [:div {:class ["col"]
                      :key (str (name col-id) "-" contractSymbol)}
                [:p (get data col-id)]])))
          doall)]))

(defn render
  []
  (let [activ-cols (get-in @state/app-state [:options :columns])]
    [:<>
     (opt-sidebar)
     [:div {:class ["options"]}
      [:div {:class ["row" "header"]}
       (->> columns-w-names
            (keep
             (fn [[col-id descr abbrev]]
               (when (contains? activ-cols col-id)
                 [:div {:class ["col"]
                        :key (str (name col-id) "-header")}
                  [:p abbrev [:span descr]]]))))]
      (->> (get-in @state/app-state [:options :data :options])
           (map row)
           doall)]]))

(ns synfron.home
  (:require
   [clojure.string :as str]
   [oz.core :as oc]
   [synfron.options :as options]
   [synfron.state :as state]))

(defn from-ts
  [ts]
  (-> ts
      (* 1000)
      (js/Date.)
      (.toLocaleString "en-US")))

(defn render-empty
  []
  [:div.empty-wrapper
   [:div.empty
    [:p "You are not tracking any options yet, start by "
     [:span.click {:on-click (fn [] (state/swap-view :search))} "searching"]
     " and "
     [:button {:class ["follow"]} "following"]
     " one."]]])

(def right-color "#a51140")
(defn render-graph
  [cs {:keys [data left right]}]
  (let [id-prefix (str "chart-" cs "-")
        data (doall (map (fn [d] (update d :timestamp from-ts)) data))]
    [:<>
     [:input {:type "checkbox" :class ["hidden"]
              :id (str id-prefix "check") :default-checked :checked}]
     [:div.chart
      [:div.chart-top
       [:select
        {:value (or left "")
         :on-change (fn [ev] (state/toggle-chart cs :left (.. ev -target -value)))}
        [:option {:value "bid"} "Bid"]
        [:option {:value "volume"} "Volume"]
        [:option {:value "openInterest"} "Open Interest"]
        [:option {:value "impliedVolatility"} "Implied Volatility"]
        [:option {:value "regularMarketPrice"} "Stock Price"]]
       [:select
        {:value (or right "")
         :on-change (fn [ev] (state/toggle-chart cs :right (.. ev -target -value)))}
        [:option {:value "delta"} "Delta"]
        [:option {:value "gamma"} "Gamma"]
        [:option {:value "theta"} "Theta"]
        [:option {:value "vega"} "Veta"]
        [:option {:value "impliedVolatility"} "Implied Volatility"]]]
      [oc/vega-lite
       {:data {:values data},
        :vconcat
        [{:width 600,
          :encoding {:x
                     {:field "timestamp",
                      :type "temporal",
                      :scale {:domain {:selection "brush", :encoding "x"}},
                      :axis {:title ""}}}
          :layer [{:mark "area",
                   :encoding
                   {:y
                    {:field left,
                     :type "quantitative",
                     :scale {:domain {:selection "brush", :encoding "y"}}}}}
                  {:mark {:type "line" :stroke right-color
                          :point {:filled false :fill "white" :stroke right-color}
                          :interpolate "monotone"}
                   :encoding
                   {:y
                    {:field right,
                     :type "quantitative", 
                     :axis {:tickCount 3, :grid false :titleColor right-color}}}}]
          :resolve {:scale {:y "independent"}}}
         {:width 600,
          :height 60,
          :mark "area",
          :selection {:brush {:type "interval", :encodings ["x" "y"]}},
          :encoding
          {:x {:field "timestamp", :type "temporal"},
           :y
           {:field left,
            :type "quantitative", 
            :axis {:tickCount 3, :grid false}}}}]}]]]))

(defn row
  [{:keys [contractSymbol] :as data}]
  (let [activ-cols (get-in @state/app-state [:home :columns])
        activ-spread? (contains? (get-in @state/app-state [:home :spreads]) contractSymbol)
        next (when activ-spread? (options/ladder-next data))]
    [:div {:class ["options" "tracker"]}
     [:div {:class ["row" "header"]}
      [:div {:class ["cell"]}]
      (->> options/columns-w-names
           (keep
            (fn [[col-id descr abbrev]]
              (when (contains? activ-cols col-id)
                [:div {:class ["cell"]
                       :key (str (name col-id) "-header")}
                 [:p abbrev [:span descr]]]))))]
     [:div {:class ["row"]}
      [:div {:class ["cell" "buttons"]}
       [:button
        {:on-click (fn [] (options/spread-button data true))
         :class ["follow" (when activ-spread? "spread")]}
        (if activ-spread? "close" "spread")]]
      (->> options/columns-w-names
           (keep
            (fn [[col-id _ _]]
              (when (contains? activ-cols col-id)
                [:div {:class ["cell"]
                       :key (str (name col-id) "-" contractSymbol)}
                 (let [v (get data col-id)]
                   (options/draw-cell next col-id v))])))
           doall)]]))

(defn render-option
  [cs]
  (let [{:keys [symbol optType expiration] :as data}
        (get-in @state/app-state [:home :tracked-options cs])
        historical (get-in @state/app-state [:home :historical cs])]
    [:div {:class ["dash-option"]}
     [:h3
      [:p (str symbol " " optType " " (-> expiration from-ts (str/split #",") first))]
      [:button.follow {:on-click (fn [] (state/toggle-tracked-options cs data))} "forget"]
      [:label {:for (str "chart-" cs "-check") :class ["follow"]} "toggle charts"]]
     (row data)
     (render-graph cs historical)]))

(defn render-tracked
  []
  (reduce
   (fn [acc cs]
     (conj acc (render-option cs)))
   [:<>]
   (-> @state/app-state :home :historical keys)))

(defn render
  []
  (let [home (:home @state/app-state)]
    (if (empty? (:tracked-options home))
      (render-empty)
      (render-tracked))))

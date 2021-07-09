(ns synfron.filters
  (:require
   [clojure.string :as str]
   [goog.dom :as gdom]
   [synfron.filters-def :as defs]
   [synfron.state :as state]))

(defn clear-filter
  []
  (doseq [el (array-seq (.getElementsByTagName js/document "input"))]
    (if (= (.-type el) "checkbox")
      (set! (.-checked el) false)
      (set! (.-value el) "")))
  (state/set-cur-filter {}))

(defn load-filter
  [f-data]
  (clear-filter)
  (state/set-cur-filter f-data))

(defn trigger-search
  ([] (trigger-search nil))
  ([offset]
   (let [filter-data (get-in @state/app-state [:filters :values])
         filter-data (if (number? offset)
                       (assoc filter-data :offset offset)
                       filter-data)
         clean-filter (->> filter-data
                           (keep
                            (fn [[k v]]
                              (when (and v (not= v ""))
                                [k
                                 (if (boolean? v)
                                   v
                                   (try (js/parseFloat v) (catch js/Error _ 0)))])))
                           (into {}))]
     (.postMessage state/worker (clj->js {:message "search" :data clean-filter})))))

(defn collect-filter
  []
  (let [vals
        (map
         (fn [el]
           [(.-id el)
            (if (= (.-type el) "checkbox")
              (.-checked el)
              (.-value el))])
         (array-seq (.getElementsByTagName js/document "input")))]
    (into {} vals)))

(defn save-filter
  []
  (let [filter-data (collect-filter)
        filter-title (js/prompt "Enter filter title")]
    (when filter-title
      (state/save-filter filter-title filter-data)
      (state/trigger-alert :success (str "Filter " filter-title " saved.")))))

(defmulti render-filter :type)

(defmethod render-filter :checkboxes
  [{f-title :title entries :entries}]
  (let [lc-f-title (str/lower-case f-title)
        f-search (get-in @state/app-state [:filters :search])
        cur-vals (get-in @state/app-state [:filters :values])]
    [:div {:class ["filter"
                   (when (and f-search
                              (not
                               (str/includes?
                                lc-f-title (str/lower-case f-search))))
                     "hidden")]}
     [:h3 {:class ["title"]} f-title]
     (reduce
      (fn [acc {c-name :name descr :descr id :id}]
        (let [cur-val (get cur-vals id true)]
          (conj
           acc
           [:div {:class ["checkbox"]}
            [:label {:for id} c-name]
            [:input {:type "checkbox" :id id
                     :on-change
                     (fn [e]
                       (state/update-cur-filter id (.. e -target -checked)))
                     :checked cur-val}]])))
      [:div {:class ["criterias"]}]
      entries)]))

(defmethod render-filter :min-max
  [{f-title :title descr :descr id :id}]
  (let [lc-f-title (str/lower-case f-title)
        min-id (str "min-" id)
        max-id (str "max-" id)
        f-search (get-in @state/app-state [:filters :search])
        cur-vals (get-in @state/app-state [:filters :values])
        min-v (str (get cur-vals min-id ""))
        max-v (str (get cur-vals max-id ""))]
    [:div {:class ["filter"
                   (when (and f-search
                              (not
                               (str/includes?
                                lc-f-title (str/lower-case f-search))))
                     "hidden")]}
     [:div {:class ["title"]}
      [:h3 f-title]
      (when descr [:p descr])]
     [:div {:class ["criterias"]}
      [:label {:for min-id} "from"]
      [:input {:type "number" :step 0.01 :id min-id :value min-v
               :on-change (fn [ev]
                            (state/update-cur-filter
                             min-id (.. ev -target -value)))
               :on-key-down (fn [ev]
                              (when (= (.-keyCode ev) 13)
                                (trigger-search)))}]
      [:label {:for max-id} "to"]
      [:input {:type "number" :step 0.01 :id max-id :value max-v
               :on-change (fn [ev]
                            (state/update-cur-filter
                             max-id (.. ev -target -value)))
               :on-key-down (fn [ev]
                              (when (= (.-keyCode ev) 13)
                                (trigger-search)))}]]]))

(defmethod render-filter :default
  [_] nil)

(defn filter-sidebar
  []
  (let [saved-filters (get-in @state/app-state [:filters :saved])
        sidebar (:sidebar @state/app-state)]
    [:div {:class ["sidebar" (when sidebar "show")]}
     [:div
      {:class ["sidebar-toggle"]
       :on-click state/toggle-sidebar}
      [:p (if sidebar "<" ">")]]
     [:h3 "Saved filters"]
     (reduce
      (fn [acc [f-id [f-name f-data]]]
        (conj
         acc
         [:div {:class ["filter-mgmt-entry"]}
          [:p f-name]
          [:button {:on-click (fn [] (load-filter f-data))} "Load"]
          [:button {:on-click (fn [] (state/forget-filter f-id))} "Delete"]]))
      [:<>]
      saved-filters)]))

(defn render
  []
  [:<>
   (filter-sidebar)
   [:header {:class ["filter-header"]}
    [:div {:class ["filter-general"]}
     [:button {:on-click (fn [] (save-filter))} "Save current filter"]
     [:button
      {:on-click (fn [] (clear-filter))}
      "Clear filter"]]
    [:div {:class ["filter-search"]}
     [:label {:for "filter-search"} "Search for a filter "]
     [:input
      {:id "filter-search" :type "text" :placeholder "E.g. \"premium\""
       :on-input (fn [e] (state/swap-filter-search (.. e -target -value)))}]]]
   [:div {:class ["filters"]}
    (reduce
     #(conj %1 (render-filter %2))
     [:<>]
     defs/all-filters)]
   [:footer {:class ["filter-footer"]}
    [:div {:class ["filter-general"]}
     [:button {:on-click trigger-search} "Search"]]]])

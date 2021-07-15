(ns syncretism-client.ui
  (:require
   [clojure.data.json :as json]
   [clj-http.client :as http]
   [syncretism-client.filters-def :as fdef]
   [syncretism-client.communication :as com]
   [syncretism-client.state :as st :refer [state]])
  (:import
   [imgui.extension.implot ImPlot ImPlotStyle]
   [imgui.extension.implot.flag ImPlotStyleVar ImPlotAxisFlags ImPlotFlags]
   [imgui ImGui]
   [imgui.type ImBoolean ImString ImFloat]
   [imgui.flag ImGuiWindowFlags]
   [imgui.app Configuration Application]))

(new ImFloat Float/MAX_VALUE)

(def data
  (-> "test.json"
      slurp
      (json/read-str :key-fn keyword)))

(defn convert-data [v] (into-array java.lang.Number v))

(defn draw-plot
  [state]
  (let [{:keys [data scs]} @state
        f-ts (-> data first :timestamp)
        cs (-> data first :contractSymbol)
        x (convert-data (map :timestamp data))
        y1 (convert-data (map :bid data))
        y2 (convert-data (map :delta data))]
    (ImGui/setNextWindowSize 500.0 500.0)
    (ImGui/begin "Contract history")
    (ImGui/checkbox "test check" (:bool @state))
    (ImGui/inputText "CS" scs)
    (when (ImGui/button "reload")
      (future
        (swap! state assoc :data (com/get-contract (str scs))))
      (println "→ getting" (str scs)))
    (ImGui/text (str (:bool @state)))
    (when (ImPlot/beginPlot
           cs "days" "$" (imgui.ImVec2. 0.0 0.0)
           0
           ImPlotAxisFlags/Time
           ImPlotAxisFlags/None)
      (ImPlot/pushStyleVar ImPlotStyleVar/FillAlpha 0.5)
      (ImPlot/plotShaded "bid" x y1 0 0)
      (ImPlot/plotLine "bid" x y1)
      (ImPlot/pushStyleVar ImPlotStyleVar/FillAlpha 0.5)
      (ImPlot/plotShaded "delta" x y2 0 0)
      (ImPlot/plotLine "delta" x y2)
      (ImPlot/endPlot))
    (ImGui/end)))

(defmulti draw-filter :type)

(defmethod draw-filter :min-max
  [{title :title min-v :min max-v :max descr :descr}]
  (ImGui/text title)
  (when (ImGui/isItemHovered)
    (ImGui/beginTooltip)
    (ImGui/text descr)
    (ImGui/endTooltip))
  (ImGui/pushItemWidth 100.0)
  (ImGui/inputFloat "min" min-v)
  (ImGui/sameLine)
  (ImGui/pushItemWidth 100.0)
  (ImGui/inputFloat "max" max-v))

(defmethod draw-filter :boolean
  [{:keys [title descr value]}]
  (ImGui/checkbox title value)
  (when (ImGui/isItemHovered)
    (ImGui/beginTooltip)
    (ImGui/text descr)
    (ImGui/endTooltip)))

(defmethod draw-filter :default [_] nil)

(defn draw-filter-window
  [filter-data]
  (ImGui/begin "New Search")
  (doseq [f (vals filter-data)]
    (draw-filter f))
  (when (ImGui/button "Send")
    (future (let [clean-f (fdef/convert filter-data)
                  res (com/search clean-f)]
              (st/add-search filter-data res)
              (println "Sent" clean-f "→" (count (:options res)) "results."))))
  (when (ImGui/button "Reset")
    (st/init-filter))
  (ImGui/sameLine)
  (when (ImGui/button "Close")
    (st/clear-filter))
  (ImGui/end))

(defn draw-search-result
  [uuid [_ _ {:keys [options quotes fundamentals]}]]
  (ImGui/setNextWindowSize 500.0 600.0)
  (ImGui/begin uuid)
  (when (ImGui/beginTable "" 2)
    (ImGui/tableNextColumn)
    (ImGui/tableHeader "CS")
    (ImGui/tableNextColumn)
    (ImGui/tableHeader "Strike")
    (doseq [opt options]
      (ImGui/tableNextRow)
      (ImGui/tableSetColumnIndex 0)
      (ImGui/text (:contractSymbol opt))
      (ImGui/tableSetColumnIndex 1)
      (ImGui/text (str (:strike opt))))
    (ImGui/endTable))
  (when (ImGui/button "Close")
    (st/rm-search uuid))
  (ImGui/end))

(defn main-menu
  []
  (ImGui/beginMainMenuBar)
  (when (ImGui/beginMenu "Syncretism")
    (when (ImGui/menuItem "Search")
      (st/init-filter))
    (when (ImGui/menuItem "Quit")
      (System/exit 0))
    (ImGui/endMenu))
  (ImGui/endMainMenuBar))

(defn run
  []
  (let [sstate (atom {:bool (new ImBoolean)
                      :data data
                      :scs (new ImString)})
        app
        (proxy [Application] []
          (configure [^Configuration config] (. config setTitle "Coucou"))
          (initImGui [^Configuration config]
            (proxy-super initImGui config)
            (ImPlot/createContext))
          (process []
            (main-menu)
            (when-let [filter-data (:filter @state)]
              (draw-filter-window filter-data))
            (doseq [[uuid data] (get-in @state [:ui :searches])]
              (draw-search-result uuid data))
            (draw-plot sstate)))]
    (Application/launch app)))

;; (run)



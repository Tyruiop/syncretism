(ns syncretism-client.ui
  (:require
   [clojure.data.json :as json]
   [clj-http.client :as http])
  (:import
   [imgui.extension.implot ImPlot ImPlotStyle]
   [imgui.extension.implot.flag ImPlotStyleVar ImPlotAxisFlags ImPlotFlags]
   [imgui ImGui]
   [imgui.flag ImGuiWindowFlags]
   [imgui.app Configuration Application]))

(def data
  (-> "test.json"
      slurp
      (json/read-str :key-fn keyword)))

(defn get-contract
  [cs]
  (-> (str "https://api.syncretism.io/ops/historical/" cs)
      http/get
      :body
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
        (swap! state assoc :data (get-contract (str scs))))
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

(defn run
  []
  (let [state (atom {:bool (new imgui.type.ImBoolean)
                     :data data
                     :scs (new imgui.type.ImString)})
        app
        (proxy [Application] []
          (configure [^Configuration config] (. config setTitle "Coucou"))
          (initImGui [^Configuration config]
            (proxy-super initImGui config)
            (ImPlot/createContext))
          (process []
            (ImGui/beginMainMenuBar)
            (when (ImGui/beginMenu "Syncretism")
              (when (ImGui/menuItem "Quit")
                (System/exit 0))
              (ImGui/endMenu))
            (ImGui/endMainMenuBar)
            (ImGui/begin "Foo")
            (ImGui/text "machin")
            (ImGui/end)
            (draw-plot state)))]
    (Application/launch app)))

;; (run)



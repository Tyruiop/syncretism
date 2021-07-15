(ns syncretism-client.state
  (:require
   [clojure.java.io :as io]
   [syncretism-client.filters-def :refer [filters]])
  (:import
   [imgui.type ImBoolean ImString ImFloat]))

(def state (atom (-> "init.edn" io/resource slurp read-string)))

(defn random-uuid [] (str (java.util.UUID/randomUUID)))

(defn init-filter [] (swap! state assoc :filter filters))
(defn clear-filter [] (swap! state assoc :filter nil))
(defn update-filter [k v] (swap! state update-in [:filter k] v))

;; Start with empty filter window on
(init-filter)

(defn add-search
  [f-data data]
  (let [ts (System/currentTimeMillis)]
    (swap! state assoc-in [:ui :searches (random-uuid)] [f-data ts data])))
(defn rm-search [uuid] (swap! state update-in [:ui :searches] dissoc uuid))

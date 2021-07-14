(ns syncretism-client.state
  (:require [clojure.java.io :as io]))

(def state (atom (-> "init.edn" io/resource slurp read-string)))

(defn init-filter [] (swap! state assoc :filter {}))
(defn clear-filter [] (swap! state assoc :filter nil))
(defn update-filter [k v] (swap! state update-in [:filter k] v))

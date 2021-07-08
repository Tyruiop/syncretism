(ns synfron.options
  (:require [synfron.state :as state]))

(defn render
  []
  [:div
   (doall
    (map
     #(do [:p (str %)])
     (get-in @state/app-state [:options :data :options])))])

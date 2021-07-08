(ns synfron.core
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [synfron.state :as state]
   [synfron.ui :as ui]))

(defn init
  []
  (rdom/render
   [(fn [] [ui/main-ui])]
   (gdom/getElement "app")))

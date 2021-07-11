(ns synfron.core
  (:require
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [synfron.state :as state]
   [synfron.ui :as ui]))

(comment
  (when (.-serviceWorker js/navigator)
    (.addEventListener
     js/window
     "load"
     (fn []
       (-> js/navigator
           .-serviceWorker
           (.register "/sw.js")
           (.then
            (fn [registration]
              (.info js/console (str "ServiceWorker registration successful with scope: "
                                     (.-scope registration))))
            (fn [err]
              (.info js/console (str "ServiceWorker registration failed: " err)))))))))

(defn init
  []
  (state/load-state)
  (rdom/render
   [(fn [] [ui/main-ui])]
   (gdom/getElement "app")))

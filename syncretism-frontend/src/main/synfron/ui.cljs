(ns synfron.ui
  (:require
   [clojure.string :as str]
   [goog.dom :as gdom]
   [reagent.core :as r]

   [synfron.state :as state]
   [synfron.home :as home]
   [synfron.options :as options]
   [synfron.filters :as filters]))

(defn menu-entry
  [cur-view id]
  [:div {:class ["menu" (when (= cur-view id) "selected")]
         :on-click (fn [_] (state/swap-view id))}
   [:p (name id)]])

(defn header
  [app-state]
  (let [cur-view (:cur-view @app-state)]
    [:header {:class "top"}
     [:div {:class "top-menu"}
      (menu-entry cur-view :home)
      (menu-entry cur-view :search)
      (when (> (count (get-in @app-state [:options :data])) 0)
        (menu-entry cur-view :options))]
     [:div {:class "top-right-menu"}
      [:p [:a {:href "https://github.com/Tyruiop/syncretism" :target "_blank"} "Github"]]
      [:p [:a {:href "https://ops.syncretism.io/api.html" :target "_blank"} "API"]]
      [:p [:a {:href "https://discord.gg/qBWD5Sus3d" :target "_blank"} "Discord"]]
      [:p [:a {:href "https://www.buymeacoffee.com/syncretism" :target "_blank"}
           "Buy me a coffee"]]]
     [:div {:class "title"}
      [:h1 "Ops.Syncretism"]]]))

(defn footer
  []
  [:footer
   ])

(defn draw-alert
  [{:keys [class text]}]
  [:div {:class ["alert" (name class)]}
   [:p text]])

(defn main-ui []
  (r/create-class
   {:reagent-render
    (fn []
      [:<>
       (header state/app-state)
       [:main
        (case (:cur-view @state/app-state)
          :home (home/render)
          :options (options/render)
          :search (filters/render))]
       (footer)
       (when-let [alert (:alert @state/app-state)]
         (draw-alert alert))])

    :display-name "main"

    :component-did-mount
    (fn [this]
      "")}))

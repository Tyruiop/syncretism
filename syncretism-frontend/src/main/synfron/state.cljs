(ns synfron.state
  (:require [reagent.core :as r]))

(def app-state
  (r/atom
   {;; :home | :options | :search
    :cur-view :search

    :home
    {:tracked-options #{}
     :data []}

    :filters
    {;; Current values of different filters
     :values {}
     
     ;; Are we searching for a specific filter
     :search nil

     ;; Window to load/delete existing filter
     :management false

     ;; List of saved filters
     :saved {1 ["Cheap near the money options" {"max-price" 0.05}]
             2 ["Expensive options" {"min-price" 10.0}]}}

    :options
    {;; Which options have their info box opened
     :full-view #{}
     ;; search result
     :data {}}

    :alert nil
    }))

(defn swap-view [view] (swap! app-state #(assoc % :cur-view view)))
(defn reset-alert [] (swap! app-state #(assoc % :alert nil)))
(defn trigger-alert
  [class text]
  (swap! app-state #(assoc % :alert {:class class :text text}))
  (js/setTimeout reset-alert 5000))

;; Filter functions
(defn swap-filter-search [txt]
  (swap! app-state #(assoc-in % [:filters :search] (if (= txt "") nil txt))))
(defn toggle-filter-management []
  (swap! app-state #(update-in % [:filters :management] not)))
(defn forget-filter [id]
  (swap! app-state #(update-in % [:filters :saved] dissoc id)))
(defn set-cur-filter [v]
  (swap! app-state #(assoc-in % [:filters :values] v)))
(defn update-cur-filter [k v]
  (swap! app-state #(assoc-in % [:filters :values k] v)))
(defn save-filter [title data]
  (swap! app-state #(assoc-in % [:filters :saved (str (random-uuid))] [title data])))


;; Options listing functions
(defn set-data [data]
  (swap! app-state #(assoc-in % [:options :data] data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service worker handling (communication between app & SW)
(def worker (js/Worker. "/js/worker.js"))
(defn err-message
  [message]
  (println "Unknown message:" message))
(defn parse-message
  [e]
  (let [{:keys [message data]} (js->clj (.-data e) :keywordize-keys true)]
    (case message
      "pong" (println data)
      "search" (do
                 (set-data data)
                 (swap-view :options))
      (err-message message))))
(.. worker (addEventListener "message" parse-message))

(defn test-sw []
  (.postMessage worker (clj->js {:message "ping"})))

(ns synfron.state
  (:require
   [cljs.reader :refer [read-string]]
   [reagent.core :as r]
   ["idb-keyval" :as idb]))

(def worker (js/Worker. "/js/worker.js"))

(def app-state
  (r/atom
   {;; :home | :options | :search
    :cur-view :search

    ;; Whether the sidebar is visible
    :sidebar true

    :home
    {:tracked-options {}
     :columns #{:contractSymbol :symbol :optType :strike
                :expiration :impliedVolatility :bid :ask :lastPrice
                :volume :openInterest :lastCrawl}
     :historical {}}

    :filters
    {;; Current values of different filters
     :values {}
     ;; Are we searching for a specific filter
     :search nil
     ;; Window to load/delete existing filter
     :management false
     ;; List of saved filters
     :saved {1 ["Cheap near the money options" {"max-price" 0.05 "max-diff" 10}]
             2 ["θ crush" {"min-price" 10.0 "max-exp" 60 "itm" false}]}}

    :options
    {;; Which options have their info box opened
     :infobox #{}
     ;; Which columns do we show (with sane defaults)
     :columns #{:contractSymbol :symbol :optType :strike
                :expiration :impliedVolatility :bid :ask :lastPrice
                :volume :openInterest}
     ;; search result
     :data {}
     ;; option ladders
     :ladder {}
     ;; Which options do we want to see a spread for
     :spreads #{}}

    ;; Is there an alert to display on FE.
    :alert nil
    }))

(defn print-cur-filter [] (println (get-in @app-state [:filters :values])))
(defn print-cur-hist [] (println (map #(get % "timestamp") (take 5 (get-in @app-state [:historical])))))

(defn toggle-set [s el]
  (if (contains? s el)
    (disj s el)
    (conj s el)))

(defn swap-view [view] (swap! app-state #(assoc % :cur-view view)))
(defn reset-alert [] (swap! app-state #(assoc % :alert nil)))
(defn trigger-alert
  [class text]
  (swap! app-state #(assoc % :alert {:class class :text text}))
  (js/setTimeout reset-alert 5000))
(defn toggle-sidebar [] (swap! app-state #(update % :sidebar not)))

;; To call every time the state needs to be saved locally.
(def key-name "syncretism-local")
(defn save-state
  []
  (let [clean-state (-> @app-state
                        (assoc :cur-view :home)
                        (assoc :alert nil)
                        (assoc-in [:options :data] {})
                        (assoc-in [:options :ladder] {})
                        (assoc-in [:options :spreads] #{}))]
    (->
     (js/Promise.resolve
      (idb/set key-name (pr-str clean-state)))
     (.then (fn [_] (trigger-alert :success "Saved user state."))))))
;; To load
(defn load-state
  []
  (-> (idb/get key-name)
      (.then
       (fn [data]
         (when data
           (reset! app-state (read-string data)))))
      (.catch
       (fn [err]
         (.log js/console "No data saved.")))))

;; Filter functions
(defn swap-filter-search [txt]
  (swap! app-state #(assoc-in % [:filters :search] (if (= txt "") nil txt))))
(defn forget-filter [id]
  (swap! app-state #(update-in % [:filters :saved] dissoc id))
  (save-state))
(defn set-cur-filter [v]
  (swap! app-state #(assoc-in % [:filters :values] v)))
(defn update-cur-filter [k v]
  (swap! app-state #(assoc-in % [:filters :values k] v)))
(defn save-filter [title data]
  (swap! app-state #(assoc-in % [:filters :saved (str (random-uuid))] [title data]))
  (save-state))


;; Options listing functions
(defn toggle-column [col-id]
  (swap! app-state #(update-in % [:options :columns] toggle-set col-id))
  (save-state))
(defn set-data [data]
  (swap! app-state #(assoc-in % [:options :data] data)))
(defn append-data
  [{:keys [catalysts options quotes]}]
  (swap! app-state #(update-in % [:options :data :catalysts] merge catalysts))
  (swap! app-state #(update-in % [:options :data :quotes] merge quotes))
  (swap! app-state (fn [old]
                     (update-in
                      old [:options :data :options]
                      #(-> %
                           (into options)
                           distinct)))))
(defn toggle-spread [cs]
  (swap! app-state #(update-in % [:options :spreads] toggle-set cs)))
(defn toggle-tracked-options [cs data]
  (if (contains? (get-in @app-state [:home :tracked-options]) cs)
    (do
      (swap! app-state #(update-in % [:home :tracked-options] dissoc cs))
      (swap! app-state #(update-in % [:home :historical] dissoc cs)))
    (do
      (swap! app-state #(update-in % [:home :tracked-options] assoc cs data))
      (.postMessage worker (clj->js {:message "historical" :data cs}))))
  (save-state))


;; Dashboard functions
(defn init-historical [cs data]
  (swap! app-state
         #(assoc-in % [:home :historical cs] {:data data :left "bid" :right "delta"}))
  (save-state))
(defn toggle-chart [cs side v]
  (swap! app-state #(assoc-in % [:home :historical cs side] v)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service worker handling (communication between app & SW)
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
      "search-append" (append-data data)
      "ladder" (let [[ladder-def ladder-data] data]
                 (swap! app-state #(assoc-in % [:options :ladder ladder-def] ladder-data)))
      "contract" (let [[cs cs-data] data]
                   (swap! app-state #(assoc-in % [:home :tracked-options cs] data)))
      "historical" (let [[cs cs-data] data]
                     (init-historical cs cs-data))
      (err-message message))))
(.. worker (addEventListener "message" parse-message))

(defn test-sw []
  (.postMessage worker (clj->js {:message "ping"})))

(ns synfron.worker
  (:require
   [synfron.communication :as com]))

;; Do we need a web worker? Definitely not. Are we using one nonetheless? Yes.

(println "Web worker on.")

(defn err-message
  [message]
  (println "Unknown message:" message))

(defn search
  [filter-data]
  (println "Sending search" filter-data)
  (let [append? (and (number? (:offset filter-data)) (> (:offset filter-data) 0))
        proc-fn (fn [res]
                  (js/postMessage (clj->js {:message
                                            (if append?
                                              "search-append"
                                              "search")
                                            :data res})))]
    (com/search filter-data proc-fn)))

(defn ladder
  [ladder-def]
  (println "Getting ladder" ladder-def)
  (let [proc-fn (fn [res] (js/postMessage (clj->js {:message "ladder" :data res})))]
    (com/get-ladder ladder-def proc-fn)))

(defn init []
  (js/self.addEventListener
   "message"
   (fn [^js e]
     (let [{:keys [message data]} (js->clj (.-data e) :keywordize-keys true)]
       (case message
         "ping" (js/postMessage (clj->js {:message "pong"}))
         "search" (search data)
         "ladder" (ladder data)
         (err-message message))))))

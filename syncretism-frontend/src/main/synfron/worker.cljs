(ns synfron.worker
  (:require
   [synfron.communication :as com]))

(defn err-message
  [message]
  (println "Unknown message:" message))

(defn init []
  (js/self.addEventListener
   "message"
   (fn [^js e]
     (let [{:keys [message data]} (js->clj (.-data e) :keywordize-keys true)]
       (case message
         (err-message message))))))

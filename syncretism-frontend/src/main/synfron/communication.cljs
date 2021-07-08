(ns synfron.communication
  (:require
   [goog.dom :as gdom]
   [cljs-http.client :as http]
   [cljs.reader :refer [read-string]]
   [cljs.core.async :as async :refer [<! go]]))

;; (def srv-addr "https://api.syncretism.io")
(def srv-addr "http://localhost:3000")

(defn search
  [params]
  (go
    (let [resp
          (<! (http/post
               (str srv-addr "/ops")
               {:with-credentials? false
                :json-params params}))
          {:keys [quotes options catalysts]} {}
          res (-> resp :body js/JSON.parse (js->clj :keywordize-keys true))]
      res)))

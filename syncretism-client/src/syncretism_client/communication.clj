(ns syncretism-client.communication
  (:require
   [clojure.data.json :as json]
   [clj-http.client :as http]
   [syncretism-client.state :as state]))

(defn get-contract
  [cs]
  (-> (str (:server @state/state) "/ops/historical/" cs)
      http/get
      :body
      (json/read-str :key-fn keyword)))

(defn search
  [filters]
  (-> (str (:server @state/state) "/ops")
      (http/post {:body (json/write-str filters)
                  :header {"Content-Type" "application/json"}})
      :body
      (json/read-str :key-fn keyword)))

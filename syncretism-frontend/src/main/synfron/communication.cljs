(ns synfron.communication
  (:require
   [goog.dom :as gdom]
   [cljs-http.client :as http]
   [cljs.reader :refer [read-string]]
   [cljs.core.async :as async :refer [<! go]]))

;; (def srv-addr "https://api.syncretism.io")
(def srv-addr "http://localhost:3000")

(defn get-market-status
  []
  (go
    (let [resp
          (<! (http/get (str srv-addr "/market/status")
                        {:with-credentials? false}))]
      (-> resp :body read-string :status))))

(defn get-ladder
  [[ticker expiration opttype] proc-fn]
  (go
    (let [resp
          (<! (http/get
               (str srv-addr "/ops/ladder/" ticker "/" opttype "/" expiration)
               {:with-credentials? false}))
          data (->> resp
                    :body
                    (.parse js/JSON))
          clj-data (js->clj data :keywordize-keys true)
          ladder (->> clj-data
                      (map
                       (fn [{:keys [contractSymbol] :as d}]
                         [contractSymbol d]))
                      (into {}))]
      (proc-fn {[ticker expiration opttype] ladder}))))

(defn search
  [params proc-fn]
  (go
    (let [resp
          (<! (http/post
               (str srv-addr "/ops")
               {:with-credentials? false
                :json-params params}))]
      (-> resp :body js/JSON.parse (js->clj :keywordize-keys true) proc-fn))))

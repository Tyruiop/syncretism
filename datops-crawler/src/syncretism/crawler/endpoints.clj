(ns syncretism.crawler.endpoints
  (:require
   [taoensso.timbre :as timbre :refer [info warn error]]))

;; All query endpoints (query1 + query2) and all proxies
(def y-eps
  ["https://query1.finance.yahoo.com/v7/finance/options/"
   "https://query2.finance.yahoo.com/v7/finance/options/"])

;; List of all possible endpoints based on available proxies
(def all-endpoints
  (let [proxies (-> "resources/proxies.edn" slurp read-string)]
    (info (str "Loaded " (count proxies) " proxies"))
    (->> y-eps
         (mapcat
          (fn [endpoint]
            (map (fn [proxy] [{:endpoint endpoint :proxy proxy}  {:failures 0}]) proxies)))
         (into {})
         atom)))

(def available-endpoints (atom (keys @all-endpoints)))

(defn register-failure
  "If a proxy or endpoint starts dying register its failure"
  [endpoint]
  (swap! all-endpoints #(update-in % [endpoint :failures] inc)))

(defn sort-endpoints
  "If a proxy or endpoint starts dying, move it back at the end of the queue of available
  endpoints."
  []
  (warn (str "Resetting endpoints"))
  (let [all-eps (map first (sort-by #(-> % second :failures) @all-endpoints))]
    (reset! available-endpoints all-eps)))

(ns datops-daily.db-update
  (:require
   [clojure.java.jdbc :as db]
   [clojure.data.json :as json]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as jdbp]
   [next.jdbc.types :as jdbt]))

(def month-unit (* 3600 24 30))

(defn calculate-monthly-yield
  "Calculate the yield (premium/strike) and monthly yield of a given option"
  [{:keys [contractsymbol strike ask bid lastprice expiration]}]
  (when (and (> strike 0) (or (> ask 0) (> lastprice 0)))
    (let [yield (/ (or (when (> ask 0) ask) lastprice) strike)
          now (/ (System/currentTimeMillis) 1000)
          nb-months (/ (- expiration now) month-unit)]
      [(float yield) (when (> nb-months 0) (float (/ yield nb-months))) contractsymbol])))

;; (calculate-monthly-yield {:strike 100 :ask 3 :expiration (+ (* 3 month-unit) (/ (System/currentTimeMillis) 1000))})
;; => [0.03 0.01 nil]

(def db (-> "resources/db.edn" slurp read-string))

(defn get-live-options
  "Allow to get options with pagination"
  [limit & {:keys [last-seen]}]
  (db/query
   db
   (str "SELECT * FROM live WHERE expiration > " (int (/ (System/currentTimeMillis) 1000))
        (when last-seen
          (str " AND contractsymbol > \"" last-seen "\""))
        " ORDER BY contractsymbol LIMIT " limit)))

(defn update-live-options
  [limit & {:keys [last-seen]}]
  (let [options (get-live-options limit :last-seen last-seen)
        yields (keep calculate-monthly-yield options)
        last-seen (-> options last :contractsymbol)]
    (with-open [con (jdbc/get-connection db)
                ps
                (jdbc/prepare
                 con
                 ["UPDATE live SET yield=?, monthlyyield=? WHERE contractsymbol=?"])]
      (jdbp/execute-batch! ps yields))
    (when (not-empty yields)
      (recur limit {:last-seen last-seen}))))

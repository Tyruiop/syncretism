(ns datops-compute.db
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.java.jdbc :as db]
   [taoensso.timbre :as timbre :refer [info warn error]]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as jdbp]
   [datops-compute.yield :refer [calculate-monthly-yield]]))

(def db (-> "db.edn" io/resource slurp read-string))

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
  (info (str "Last seen: " last-seen))
  (let [options (get-live-options limit :last-seen last-seen)
        yields (keep calculate-monthly-yield options)]
    (with-open [con (jdbc/get-connection db)
                ps
                (jdbc/prepare
                 con
                 ["UPDATE live SET yield=?, monthlyyield=? WHERE contractsymbol=?"])]
      (jdbp/execute-batch! ps yields))
    (when (not-empty options)
      (recur limit {:last-seen (-> options last :contractsymbol)}))))

(defn timeseries-table-def
  []
  (jdbc/execute!
   db
   ["create table if not exists timeseries (
  contractSymbol VARCHAR(40) not null,
  timestamp INT not null,
  ask FLOAT,
  bid FLOAT,
  impliedVolatility FLOAT,
  volume DOUBLE,
  openInterest DOUBLE,
  delta FLOAT,
  gamma FLOAT,
  theta FLOAT,
  vega FLOAT,
  regularMarketPrice DOUBLE,
  regularMarketVolume DOUBLE,
  regularMarketChange DOUBLE,
  marketCap DOUBLE,
  PRIMARY KEY (contractSymbol, timestamp)
  ) ENGINE=Aria;"]))

(def ts-columns
  ["contractSymbol"
   "timestamp"
   "ask"
   "bid"
   "impliedVolatility"
   "volume"
   "openInterest"
   "delta"
   "gamma"
   "theta"
   "vega"
   "regularMarketPrice"
   "regularMarketVolume"
   "regularMarketChange"
   "marketCap"])

(def ts-columns-nb (count ts-columns))

(defn write-series
  [data]
  (with-open [con (jdbc/get-connection db)
              ps
              (jdbc/prepare
               con
               [(str
                 "INSERT INTO timeseries ("
                 (str/join ", " ts-columns)
                 ") VALUES (" (str/join ", " (repeat ts-columns-nb "?")) ")"
                 " ON DUPLICATE KEY UPDATE "
                 (str/join ", " (map #(str % "= ?") (drop 2 ts-columns))))])]
    (jdbp/execute-batch! ps data)))

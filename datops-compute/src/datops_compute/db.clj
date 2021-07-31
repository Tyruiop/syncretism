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

(defn get-averages
  [nb-days]
  (let [t (int (/ (System/currentTimeMillis) 1000))
        min-t (- t (* nb-days 3600 24))]
    (with-open [con (jdbc/get-connection db)]
      (jdbc/execute!
       con
       ["SELECT timeseries.contractSymbol,
AVG(timeseries.volume) AS volume,
AVG(timeseries.openinterest) AS oi,
AVG(timeseries.impliedvolatility) AS iv,
AVG(timeseries.premium) AS pre,
AVG(timeseries.bid) AS bid,
AVG(timeseries.delta) AS delta,
AVG(timeseries.gamma) AS gamma,
AVG(timeseries.theta) AS theta,
AVG(timeseries.vega) AS vega,
AVG(timeseries.rho) AS rho
FROM timeseries LEFT JOIN live ON timeseries.contractsymbol = live.contractsymbol WHERE live.expiration > ? AND timeseries.timestamp > ? GROUP BY timeseries.contractSymbol"
        t min-t]))))

(defn float-or-nil [v] (try (float v) (catch Exception _ nil)))

(defn flatten-vec
  [v]
  [(float-or-nil (get v :volume))
   (float-or-nil (get v :oi))
   (float-or-nil (get v :iv))
   (float-or-nil (get v :pre))
   (float-or-nil (get v :bid))
   (float-or-nil (get v :delta))
   (float-or-nil (get v :gamma))
   (float-or-nil (get v :theta))
   (float-or-nil (get v :vega))
   (float-or-nil (get v :rho))
   (get v :timeseries/contractSymbol)])

(defn write-averages-20
  []
  (let [avgs (get-averages 20)
        avgs-flat (map flatten-vec avgs)]
    (println avgs-flat)
    (with-open [con (jdbc/get-connection db)
                ps
                (jdbc/prepare
                 con
                 ["UPDATE live SET vol20d=?, oi20d=?, iv20d=?, pr20d=?,
bid20d=?, delta20d=?, gamma20d=?, theta20d=?, vega20d=?, rho20d=?
 WHERE contractsymbol=?"])]
      (jdbp/execute-batch! ps avgs-flat))))

(defn write-averages-100
  []
  (let [avgs (get-averages 100)
        avgs-flat (map flatten-vec avgs)]
    (with-open [con (jdbc/get-connection db)
                ps
                (jdbc/prepare
                 con
                 ["UPDATE live SET vol100d=?, oi100d=?, iv100d=?, pr100d=?,
bid100d=?, delta100d=?, gamma100d=?, theta100d=?, vega100d=?, rho100d=?
 WHERE contractsymbol=?"])]
      (jdbp/execute-batch! ps avgs-flat))))

(defn timeseries-table-def
  []
  (jdbc/execute!
   db
   ["create table if not exists timeseries (
  contractSymbol VARCHAR(40) not null,
  timestamp INT not null,
  premium FLOAT,
  ask FLOAT,
  bid FLOAT,
  impliedVolatility FLOAT,
  volume DOUBLE,
  openInterest DOUBLE,
  delta FLOAT,
  gamma FLOAT,
  theta FLOAT,
  vega FLOAT,
  rho FLOAT,
  dividendYield FLOAT,
  regularMarketPrice DOUBLE,
  regularMarketVolume DOUBLE,
  regularMarketChange DOUBLE,
  marketCap DOUBLE,
  rfr FLOAT,
  PRIMARY KEY (contractSymbol, timestamp)
  ) ENGINE=Aria;"]))

(def ts-columns
  ["contractSymbol"
   "timestamp"
   "premium"
   "ask"
   "bid"
   "impliedVolatility"
   "volume"
   "openInterest"
   "delta"
   "gamma"
   "theta"
   "vega"
   "rho"
   "dividendYield"
   "regularMarketPrice"
   "regularMarketVolume"
   "regularMarketChange"
   "marketCap"
   "rfr"])

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

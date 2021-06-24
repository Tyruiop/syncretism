(ns datops.db
  (:require
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as jdbp]
   [next.jdbc.types :as jdbt]))

(def db (-> "resources/db.edn" slurp read-string))

(def fundamentals-table-def
  "create table if not exists fundamentals (
symbol VARCHAR(10) primary key not null,
data JSON not null
) ENGINE=Aria;")

(defn insert-or-update-fundamentals
  [symb data]
  (jdbc/execute!
   db
   [(str "INSERT INTO fundamentals VALUES (" (pr-str symb) ", " (pr-str data) ") 
 ON DUPLICATE KEY UPDATE data=" (pr-str data))]))

(def live-quote-table-def
  "create table if not exists live_quote (
symbol VARCHAR(10) primary key not null,
data JSON not null
) ENGINE=Aria;")

(defn insert-or-update-live-quote
  [symb data]
  (jdbc/execute!
   db
   [(str "INSERT INTO live_quote VALUES (" (pr-str symb) ", " (pr-str data) ") 
 ON DUPLICATE KEY UPDATE data=" (pr-str data))]))

(def live-table-def
  "create table if not exists live (
contractSymbol VARCHAR(40) primary key not null,
symbol VARCHAR(10) not null,
lastCrawl BIGINT not null,
optType CHAR(1) not null,
strike FLOAT not null,
expiration BIGINT not null,
inTheMoney BOOLEAN,
lastPrice FLOAT not null,
pChange FLOAT not null,
volume INT,
openInterest INT,
impliedVolatility FLOAT not null,
regularMarketPrice FLOAT not null,
regularMarketDayHigh FLOAT not null,
regularMarketDayLow FLOAT not null,
priceToBook FLOAT,
ask FLOAT,
bid FLOAT,
lastTradeDate BIGINT,
quoteType VARCHAR(15)
) ENGINE=Aria;")

;; (jdbc/execute! db ["DROP TABLE live"])
;; (jdbc/execute! db [live-table-def])
;; (jdbc/execute! db [live-quote-table-def])
;; (jdbc/execute! db ["ALTER TABLE live ADD COLUMN (quoteType VARCHAR(15))"])
;; (jdbc/execute! db ["ALTER TABLE live ADD COLUMN (regularMarketPrice FLOAT)"])

(def live-columns
  [[[:opt :contractSymbol] "contractSymbol"]
   [[:quote :symbol] "symbol"]
   [[:req-time] "lastCrawl"]
   [[:opt :opt-type] "optType"]
   [[:opt :strike] "strike"]
   [[:opt :expiration] "expiration"]
   [[:opt :inTheMoney] "inTheMoney"]
   [[:opt :lastPrice] "lastPrice"]
   [[:opt :change] "pChange"]
   [[:opt :volume] "volume"]
   [[:opt :openInterest] "openInterest"]
   [[:opt :impliedVolatility] "impliedVolatility"]
   [[:quote :regularMarketPrice] "regularMarketPrice"]
   [[:quote :regularMarketDayHigh] "regularMarketDayHigh"]
   [[:quote :regularMarketDayLow] "regularMarketDayLow"]
   [[:quote :priceToBook] "priceToBook"]
   [[:opt :ask] "ask"]
   [[:opt :bid] "bid"]
   [[:opt :lastTradeDate] "lastTradeDate"]
   [[:opt :quote-type] "quoteType"]])

(def live-columns-nb (count live-columns))

(defn clean-live-data
  [data]
  (let [cleaned-cols
        (map
         (fn [[path _]]
           (let [d (get-in data path)]
             d))
         live-columns)]
    (into [] (concat cleaned-cols (rest cleaned-cols)))))

(defn insert-or-update-live
  [data]
  (let [cleaned-data (map clean-live-data data)]
    (with-open [con (jdbc/get-connection db)
                ps
                (jdbc/prepare
                 con
                 [(str
                   "INSERT INTO live ("
                   (str/join ", " (map last live-columns))
                   ") VALUES ("
                   (str/join ", " (repeat live-columns-nb "?")) ")"
                   " ON DUPLICATE KEY UPDATE "
                   (str/join ", " (map #(str (last %) "=?") (rest live-columns))))])]
      (jdbp/execute-batch! ps cleaned-data))))

(defn init-db
  []
  (try
    (jdbc/execute! db [fundamentals-table-def])
    (println "Created fundamentals table")
    (catch Exception e (println e)))
  (try
    (jdbc/execute! db [live-quote-table-def])
    (println "Created live quote table")
    (catch Exception e (println e)))
  (try
    (jdbc/execute! db [live-table-def])
    (println "Created live (options) table")
    (catch Exception e (println e))))

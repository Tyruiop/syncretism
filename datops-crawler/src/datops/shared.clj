(ns datops.shared
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.data.csv :as csv]
   [taoensso.timbre :as timbre :refer [info warn error]]
   [taoensso.timbre.appenders.core :as appenders]
   [miner.ftp :as ftp]))

(timbre/merge-config!
 {:appenders {:println {:enabled? false}
              :spit (appenders/spit-appender {:fname "opts-crawler.log"})}})

(def config (-> "resources/config.edn" slurp read-string))

;; GETTING SYMBOLS LIST
;; --------------------
;; All date is sourced from http://www.nasdaqtrader.com/trader.aspx?id=symboldirdefs
;; Start by downloading locally the nasdaqlisted file.
(defn download-lists
  []
  (ftp/with-ftp [client "ftp://anonymous:@ftp.nasdaqtrader.com/SymbolDirectory"]
    (ftp/client-get client "nasdaqlisted.txt")
    (ftp/client-get client "otherlisted.txt")))

;; NASDAQ
;; ------
;; Looking at columns 2, 3, and 4 (Category and Financial Status), additionally 0 is symbol
;; For column 2 (consider all)
;; Q = NASDAQ Global Select MarketSM
;; G = NASDAQ Global MarketSM
;; S = NASDAQ Capital Market
;; For column 3, must be N (not test)
;; For column 4 (consider only D and N for now)
;; D = Deficient: Issuer Failed to Meet NASDAQ Continued Listing Requirements
;; E = Delinquent: Issuer Missed Regulatory Filing Deadline
;; Q = Bankrupt: Issuer Has Filed for Bankruptcy
;; N = Normal (Default): Issuer Is NOT Deficient, Delinquent, or Bankrupt.
;; G = Deficient and Bankrupt
;; H = Deficient and Delinquent
;; J = Delinquent and Bankrupt
;; K = Deficient, Delinquent, and Bankrupt
(defn build-nasdaq-dict
  []
  (reduce
   (fn [acc [symb sec-name _ _ status _ etf _]]
     (assoc
      acc symb
      {:name sec-name :is-etf (= etf "Y") :exchange "NASDAQ"
       :status status :exp-dates #{}}))
   {}
   (filter
    (fn [entry] (and (= "N" (nth entry 3)) (#{"D" "N"} (nth entry 4))))
    (-> "nasdaqlisted.txt" slurp (csv/read-csv :separator \|)))))

;; NYSE
;; ----
;; Column 2 are exchanges, consider them all.
;; For column 6 (test issue), must be N
;; For column 4 (etf), Y|N
(defn build-nyse-dict
  []
  (reduce
   (fn [acc [symb sec-name exchange _ etf _ _ _]]
     (assoc acc symb {:name sec-name :is-etf (= etf "Y") :exchange exchange :exp-dates #{}}))
   {}
   (filter
    (fn [entry] (= "N" (nth entry 6)))
    (-> "otherlisted.txt" slurp (csv/read-csv :separator \|)))))

;; Symbols Map
;; -----------
;; Contains the list of symbols to browse, as well as their different expirations
;; and extra metadata. Built on top of the NYSE and NASDAQ extracted data.
;; `update-tickers-dict` is used to refresh the map
(def symbols (atom {}))
(defn new-tickers-dict
  [old]
  (info (str "Requesting NYSE and NASDAQ symbols update"))
  (download-lists)
  (let [new-dict (merge (build-nasdaq-dict) (build-nyse-dict))
        old-symbols (->> old keys (into #{}))
        new-symbols (->> new-dict keys (into #{}))
        deleted-symbols (set/difference old-symbols new-symbols)]
    (when (not-empty deleted-symbols)
      (info (str "Deleted symbols: " (str/join ", " deleted-symbols))))
    {:symbols (merge-with merge new-dict (reduce dissoc old deleted-symbols))
     :new (set/difference new-symbols old-symbols)
     :deleted deleted-symbols}))
(defn update-tickers-dict []
  (let [update-result (new-tickers-dict @symbols)]
    (reset! symbols (:symbols update-result))
    (info (str "Symbols updated, " (count @symbols) " symbols tracked."))
    update-result))

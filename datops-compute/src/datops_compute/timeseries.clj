(ns datops-compute.timeseries
  (:require
   [clojure.java.io :as io]
   [com.climate.claypoole :as cp]
   [datops-compute.utils :as utils]

   [distributions.core :as dc]))

;; We use https://www.macroption.com/black-scholes-formula/ as reference

(defn parse-line
  [{:keys [opt quote req-time]}]
  {:time req-time
   :contractsymbol (:contractSymbol opt)
   :data {:oi (:openInterest opt)
          :v (:volume opt)
          :ask (:ask opt)
          :bid (:bid opt)
          :expiration (:expiration opt)
          :strike (:strike opt)
          :iv (:impliedVolatility opt)
          :last-price (:lastPrice opt)
          :stock-price (:regularMarketPrice quote)
          :prev-close (:regularMarketPreviousClose quote)
          :prev-open (:regularMarketOpen quote)
          :annual-dividend-yield (:tailingAnnualDividendYield quote)
          :annual-dividend-rate (:trailingAnnualDividendRate quote)}})

(defn aggregate-ticker
  [options-path ticker nb-days]
  (let [options (io/file options-path)
        selected-days (->> options
                           file-seq
                           rest
                           (filter #(.isDirectory %))
                           sort
                           reverse
                           (take nb-days))]
    (->> selected-days
         (pmap
          (fn [f]
            (utils/read-gzipped
             (comp parse-line read-string)
             (str f "/" ticker ".txt.gz"))))
         (mapcat identity)
         (group-by :contractsymbol)
         doall)))

(defn process-option
  [[contract data]]
  (let [sdata (sort-by :time data)
        start-ts (-> sdata first :time)
        end-ts (-> sdata last :time)]
    (reduce
     (fn [acc [d1 d2]]
       (let [d1 (:data d1)
             d2 (:data d2)
             p1 (or (:stock-price d1) (:prev-close d1) (:prev-open d1))
             p2 (or (:stock-price d2) (:prev-close d2) (:prev-open d2))]
         (+ acc (Math/pow (- (Math/log p1) (Math/log p2)) 2))))
     0
     (partition 2 1 sdata))
    sdata))

;; (def testdd (time (aggregate-ticker "./aapl/options/" "AAPL" 2)))

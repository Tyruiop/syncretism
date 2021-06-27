(ns datops-compute.timeseries
  (:require
   [clojure.java.io :as io]
   [com.climate.claypoole :as cp]
   [datops-compute.utils :as utils]))

;; We use https://www.macroption.com/black-scholes-formula/ as reference

(defn parse-line
  [{:keys [opt quote req-time]}]
  {:time req-time
   :contractsymbol (:contractSymbol opt)
   :data {:oi (:openInterest opt)
          :v (:volume opt)
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
         doall
         (mapcat identity))))

(def testdd (time (aggregate-ticker "./nly/options/" "NLY" 10)))

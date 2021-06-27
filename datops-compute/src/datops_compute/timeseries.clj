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
         (map
          (fn [f]
            (when (.isDirectory f)
              (-> (str f "/" ticker ".txt.gz")
                  utils/read-gzipped
                  utils/read-edn-per-line))))
         (mapcat identity))))

(def testdd (time (aggregate-ticker "./nly/options/" "NLY" 10)))
(count testdd)

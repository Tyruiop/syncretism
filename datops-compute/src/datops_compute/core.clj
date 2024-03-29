(ns datops-compute.core
  (:gen-class)
  (:require
   [clojure.string :as str]
   [taoensso.timbre :as timbre :refer [info warn error]]
   [datops-compute.db :as db]
   [datops-compute.timeseries :as ts]))

(defn parse-args
  [acc [arg v & r :as args]]
  (cond (nil? arg)
        acc

        (= arg "--init-db")
        (recur (assoc acc :init-db true) (rest args))

        (= arg "--avgs-20")
        (recur (assoc acc :avgs-20 true) (rest args))

        (= arg "--avgs-100")
        (recur (assoc acc :avgs-100 true) (rest args))

        (= arg "--yields")
        (recur (assoc acc :yields true) (rest args))

        (= arg "--timeseries")
        (cond
          (= v "--all")
          (recur (assoc acc :timeseries :all) r)
          
          (or (nil? v) (str/starts-with? v "--"))
          (recur (assoc acc :timeseries []) (rest args))

          :else
          (recur (assoc acc :timeseries (str/split v #",")) r))

        (= arg "--nb-days")
        (recur (assoc acc :nb-days (Integer/parseInt v)) r)

        (= arg "--opts-path")
        (recur (assoc acc :opts-path v) r)

        :else (recur acc (rest args))))

(defn -main
  [& args]
  (let [set-args (parse-args {} args)]
    (when (:init-db set-args)
      (info "Initialize time series db")
      (db/timeseries-table-def))
    (when (contains? set-args :timeseries)
      (let [tickers (:timeseries set-args)
            nb-days (get set-args :nb-days 1)
            opts-path (get set-args :opts-path "options/")]
        (cond (= :all tickers)
              (ts/process-all opts-path)
              
              (empty? tickers)
              (ts/process-all-options opts-path nb-days)
              
              :else (ts/process-options opts-path tickers nb-days))))
    (when (:yields set-args)
      (info "Updating yields across active options.")
      (db/update-live-options 10000)
      (info "Done."))
    (when (:avgs-20 set-args)
      (info "Calculating 20 days averages")
      (db/write-averages-20)
      (info "Done."))
    (when (:avgs-100 set-args)
      (info "Calculating 100 days averages")
      (db/write-averages-100)
      (info "Done."))))

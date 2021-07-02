(ns ^{:doc "Timeseries alignment for crawled data"}
    datops-compute.timeseries
  (:require
   [clojure.java.io :as io]
   [taoensso.timbre :as timbre :refer [info warn error]]
   [com.climate.claypoole :as cp]
   [datops-compute.utils :as utils]
   [datops-compute.db :as db]
   
   [syncretism.greeks :as gks]
   [syncretism.time :refer [ts-start-of-day get-day-of-week]]))

;; We use https://www.macroption.com/black-scholes-formula/ as reference
(defn parse-line
  [{:keys [opt quote req-time :as data]}]
  (let [{:keys [delta gamma theta vega]}
        (gks/calculate-greeks
         (assoc
          opt
          :opt-type (:opt-type opt)
          :annual-dividend-rate (:trailingAnnualDividendRate quote)
          :annual-dividend-yield (:tailingAnnualDividendYield quote)
          :stock-price (:regularMarketPrice quote)))]
    [(get opt :contractSymbol)
     (get opt :opt-type)
     (get opt :strike)
     (get opt :expiration)
     req-time
     (get opt :ask)
     (get opt :bid)
     (get opt :impliedVolatility)
     (get opt :volume)
     (get opt :openInterest)
     (or (get opt :delta) delta)
     (or (get opt :gamma) gamma)
     (or (get opt :theta) theta)
     (or (get opt :vega) vega)
     (get quote :regularMarketPrice)
     (get quote :regularMarketVolume)
     (get quote :regularMarketChange)
     (get quote :marketCap)]))

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
            (try
              (utils/read-gzipped
               (comp parse-line read-string)
               (str f "/" ticker ".txt.gz"))
              (catch Exception _ []))))
         (mapcat identity)
         (group-by #(take 4 %))
         (map (fn [[idcontract data]] [idcontract (map #(drop 4 %) data)]))
         doall)))

(def market-open-mins (+ (* 9 60 60) (* 30 60)))
(def market-mid-mins (+ (* 12 60 60) (* 30 60)))
(def market-close-mins (* 16 60 60))

(defn build-steps
  "Given a starting timestamp (in seconds), finds the closest beginning
  of day, and creates regular intervals [market open, 12:30, market close] on
  which we align the data, excluding weekends.

  Return [start-point, [intervals]]"
  [starting ending]
  (let [start-s (ts-start-of-day (* starting 1000))
        days (int (Math/ceil (/ (- ending start-s) 86400)))]
    [start-s
     (reduce
      (fn [acc i]
        (let [day (* i 86400)
              day-of-week (get-day-of-week (+ start-s day))
              market-open (+ day market-open-mins)
              mid-day (+ day market-mid-mins)
              market-close (+ day market-close-mins)]
          (if (or (= 6 day-of-week) (= 7 day-of-week))
            acc
            (into acc [market-open mid-day market-close]))))
      []
      (range days))]))

;; Aligning process
;; 1. take all crawls of a given stock, sort them
;; 2. set t0 to be the start of the day of the first occurence of the option
;; 3. go through the day's timestamp
;; 3.1 closest crawl during market hours for the market opening ts
;; 3.2 interpolate two closest market hours crawl for mid-market ts
;; 3.3 closest crawl during market hours for the market close ts
;; If an option hasn't been crawled during a day, average neighboring days.
;;
;; We do it this way because pre-market and post-market crawls can contain inaccurate option
;; data

(defn average
  "Linear interpolation of two feature vectors"
  [ts d1 d2]
  (let [t1 (first d1)
        t2 (first d2)
        w1 (- 1 (/ (- ts t1) (- t2 t1)))
        w2 (- 1 (/ (- t2 ts) (- t2 t1)))]
    (into
     [(int (/ (+ (first d1) (first d2)) 2))]
     (map
      (fn [v1 v2]
        (when (and (number? v1) (number? v2))
          (+ (* w1 v1) (* w2 v2))))
      (rest d1) (rest d2)))))

(defn is-active? [[_ ask bid & _]] (and ask bid (> ask 0) (> bid 0)))

(defn align-option-data-helper
  [acc start-ts [ts & n-steps :as steps] [d1 d2 d3 :as data]]
  (if (nil? ts)
    ;; We processed all the steps, done
    acc

    (let [t1 (first d1)
          t2 (first d2)
          t3 (first d3)
          full-ts (+ ts start-ts)
          dt1 (get-day-of-week t1)
          dt2 (when d2 (get-day-of-week t2))
          dts (get-day-of-week full-ts)]
      (cond
        ;; t1    ts  
        ;; |-----|---nil end of valid data (which should be suspicious)
        (nil? d2)
        (reduce
         (fn [acc ts]
           (conj acc [ts d1]))
         acc
         steps)
        
        ;; t1    ts   t2
        ;; |-----|----|     same day
        (and  (>= full-ts t1) (<= full-ts t2) (= dt1 dt2))
        (recur (conj acc [ts (average full-ts d1 d2)]) start-ts n-steps data)

        ;; t1    ts   dc   t2
        ;; |-----|----|----|     different day, means ts is closing time
        (and  (>= full-ts t1) (>= t2 full-ts) (= dt1 dts))
        (recur (conj acc [ts d1]) start-ts n-steps data)

        ;; t1    dc   ts   t2
        ;; |-----|----|----|     different day, means ts is opening time
        (and  (>= full-ts t1) (>= t2 full-ts) (= dt2 dts))
        (recur (conj acc [ts d2]) start-ts n-steps data)

        ;; t1  dc  ts  dc  t2
        ;; |---|---|---|---|     all different days
        (and  (>= full-ts t1) (<= full-ts t2))
        (recur (conj acc [ts (average full-ts d1 d2)]) start-ts n-steps data)

        ;; ts    t1   t2
        ;; |-----|----|     means we don't have any valid value before ts
        (and (>= t1 full-ts) (>= t2 full-ts))
        (recur (conj acc [ts d1]) start-ts n-steps data)

        ;; t1    t2   ts
        ;; |-----|----|---nil  means we don't have any valid value after ts
        (and (<= t1 full-ts) (<= t2 full-ts) (nil? t3))
        (reduce
         (fn [acc ts]
           (conj acc [ts d2]))
         acc
         steps)

        ;; t1    t2   ts   t3
        ;; |-----|----|----|   move along the data
        :else (recur acc start-ts steps (rest data))
        ))))

(defn align-option-data
  [[contract data]]
  (let [sdata (->> data
                   (sort-by first)
                   (filter is-active?))
        start-date (-> sdata first first)
        end-date (-> sdata last first)]
    (when (and start-date end-date)
      (let [[start-ts steps] (build-steps start-date end-date)]
        [contract start-ts (align-option-data-helper [] start-ts steps sdata)]))))

(defn clean-feats [feats] (map #(cond
                                  (ratio? %) (float %)
                                  (integer? %) (double %)
                                  :else %) feats))

(defn process-option
  [option-path ticker nb-days]
  (let [data (doall (aggregate-ticker option-path ticker nb-days))]
    (->> data
         (pmap align-option-data)
         (keep identity)
         doall
         (mapcat
          (fn [[[cs _ _ _] start-ts data]]
            (map
             (fn [[ts feats]]
               (let [feat-vec (cons cs
                                    (cons (+ start-ts ts)
                                          (rest (clean-feats feats))))]
                 (concat feat-vec (drop 2 feat-vec)))) data)))
         db/write-series)))

(defn process-options
  [option-path tickers nb-days]
  (let [nb-tickers (count tickers)]
    (info (str "Processing " nb-tickers " tickers over " nb-days " days."))
    (doseq [[i ticker] (map-indexed (fn [i t] [i t]) tickers)]
      (info (str "Processing " ticker " (" i "/" nb-tickers ") series."))
      (let [nbs (process-option option-path ticker nb-days)]
        (info (str ticker " series done (" (apply + nbs) ")."))))))

(defn process-all-options
  "Process all available options for n days"
  [option-path nb-days]
  (let [tickers (utils/find-tickers option-path)]
    (process-options option-path tickers nb-days)))

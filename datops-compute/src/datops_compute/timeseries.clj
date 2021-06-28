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

(def testdd (time (aggregate-ticker "./clov/options/" "CLOV" 2)))

(def ddd (process-option (first (filter #(clojure.string/includes? (-> % first) "CLOV230120C00012000") testdd))))
(last ddd)
;; => {:time 1624648502, :contractsymbol "CLOV230120C00012000", :data {:v 113, :expiration 1674172800, :stock-price 12.97, :iv 0.9882813671875, :annual-dividend-rate nil, :last-price 6.3, :strike 12.0, :oi 1371, :annual-dividend-yield nil, :prev-close 13.79, :prev-open 13.43, :ask 6.6, :bid 6.0}}

(def entry (last ddd))

(def rfr 0.06)

(defn calc-annual-yield
  [{:keys [annual-dividend-yield annual-dividend-rate stock-price]}]
  (or annual-dividend-yield
      (/ (or annual-dividend-rate 0) stock-price)))

(defn expiration-years
  [t]
  (let [now (/ (System/currentTimeMillis) 1000)
        time-left (- t now)
        days (/ time-left 86400)]
    (/ days 365)))

(defn calc-d1
  "Following https://www.macroption.com/black-scholes-formula/"
  [{s0 :stock-price X :strike t :expiration v :iv :as data}]
  (let [q (calc-annual-yield data)
        t (expiration-years t)]
    (/ (+ (Math/log (/ s0 X))
          (* t (+ (- rfr q) (/ (Math/pow v 2) 2))))
       (* v (Math/sqrt t)))))

(defn calc-d2
  [d1 v t]
  (- d1 (* v (Math/sqrt t))))

(defn calc-delta-call
  [data]
  (let [d1 (calc-d1 data)
        q (calc-annual-yield data)
        t (expiration-years (:expiration data))]
    (* (Math/exp (- (* q t)))
       (dc/cdf (dc/->Normal 0 1) d1))))

(defn calc-gamma
  [{s0 :stock-price X :strike t :expiration v :iv :as data}]
  (let [d1 (calc-d1 data)
        q (calc-annual-yield data)
        t (expiration-years t)]
    (*
     (/ (Math/exp (- (* q t)))
        (* s0 v (Math/sqrt t)))
     (/ 1 (Math/sqrt (* 2 Math/PI)))
     (Math/exp (- (/ (* d1 d1) 2))))))

(defn calc-theta-call
  [{s0 :stock-price X :strike t :expiration v :iv :as data}]
  (let [t (expiration-years t)
        d1 (calc-d1 data)
        d2 (calc-d2 d1 v t)
        q (calc-annual-yield data)
        eqt (Math/exp (- (* q t)))]
    (*
     (/ 1 365)
     (+
      (- (/
          (* s0 v eqt (Math/exp (- (/ (* d1 d1) 2))))
          (* 2 (Math/sqrt t) (Math/sqrt (* 2 Math/PI)))))
      (-
       (* rfr X (Math/exp (- (* rfr t))) (dc/cdf (dc/->Normal 0 1) d2)))
      (* q s0 eqt (dc/cdf (dc/->Normal 0 1) d1))))))

(defn calc-vega
  [{s0 :stock-price X :strike t :expiration v :iv :as data}]
  (let [t (expiration-years t)
        d1 (calc-d1 data)
        q (calc-annual-yield data)
        eqt (Math/exp (- (* q t)))]
    (/
     (* s0 eqt (Math/sqrt t) (Math/exp (- (/ (* d1 d1) 2))))
     (* 100 (Math/sqrt (* 2 Math/PI))))))

(calc-d1 (:data entry))

(calc-delta-call (:data entry))
;; => 0.7753990598295412
(calc-gamma (:data entry))
;; => 0.018691798680041786
(calc-theta-call (:data entry))
;; => -0.004774392935149536
(calc-vega (:data entry))
;; => 0.04859403626518364

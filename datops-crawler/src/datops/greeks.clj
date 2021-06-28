(ns datops.greeks
  "Following https://www.macroption.com/black-scholes-formula/"
  (:require
   [distributions.core :as dc]
   [datops.shared :refer [config]]))

;; Risk free interest rate, 
(def rfr (:risk-free config))
(def cdf-normal (dc/cdf (dc/->Normal 0 1)))

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
  [{s0 :stock-price q :yield X :strike t :t v :impliedVolatility :as data}]
  (/ (+ (Math/log (/ s0 X))
        (* t (+ (- rfr q) (/ (Math/pow v 2) 2))))
     (* v (Math/sqrt t))))
(defn calc-d2 [d1 v t] (- d1 (* v (Math/sqrt t))))

(defn calc-delta
  [{q :yield d1 :d1 t :t opt-type :opt-type}]
  (cond (= opt-type "C")
        (* (Math/exp (- (* q t))) (cdf-normal d1))

        (= opt-type "P")
        (* (Math/exp (- (* q t))) (- (cdf-normal d1) 1))))

(defn calc-gamma
  [{s0 :stock-price X :strike t :t v :impliedVolatility d1 :d1 q :yield eqt :eqt :as data}]
  (*
   (/ eqt (* s0 v (Math/sqrt t)))
   (/ 1 (Math/sqrt (* 2 Math/PI)))
   (Math/exp (- (/ (* d1 d1) 2)))))

(defn calc-theta
  [{opt-type :opt-type s0 :stock-price X :strike t :t
    v :impliedVolatility d1 :d1 d2 :d2 q :yield eqt :eqt :as data}]
  (let [p1 (- (/
               (* s0 v eqt (Math/exp (- (/ (* d1 d1) 2))))
               (* 2 (Math/sqrt t) (Math/sqrt (* 2 Math/PI)))))
        p2 (* rfr X (Math/exp (- (* rfr t))))
        p3 (* q s0 eqt)]
    (*
     (/ 1 365)
     (cond (= opt-type "C")
           (+ p1 (- (* p2 (cdf-normal d2))) (* p3 (cdf-normal d1)))

           (= opt-type "P")
           (+ p1 (* p2 (cdf-normal (- d2))) (- (* p3 (cdf-normal (- d1)))))))))

(defn calc-vega
  [{s0 :stock-price X :strike t :t v :impliedVolatility d1 :d1 eqt :eqt :as data}]
  (/
   (* s0 eqt (Math/sqrt t) (Math/exp (- (/ (* d1 d1) 2))))
   (* 100 (Math/sqrt (* 2 Math/PI)))))

(defn calculate-greeks
  [{expiration :expiration v :impliedVolatility :as data}]
  (let [t (expiration-years expiration)
        q (calc-annual-yield data)
        d1 (calc-d1 (assoc data :yield q :t t))
        d2 (calc-d2 d1 v t)
        eqt (Math/exp (- (* q t)))

        ;; To avoid recalculating common values
        full-data (assoc data :d1 d1 :d2 d2 :yield q :t t :eqt eqt)]
    {:delta (calc-delta full-data)
     :gamma (calc-gamma full-data)
     :theta (calc-theta full-data)
     :vega (calc-vega full-data)}))

;; (calculate-greeks {:v 34, :expiration 1655424000, :stock-price 133.11, :iv 0.2829661547851562, :annual-dividend-rate 0.82, :last-price 18.8, :strike 125.0, :oi 10190, :annual-dividend-yield nil, :prev-close 133.41, :prev-open 133.46, :ask 18.8, :bid 18.6 :opt-type "C"})
;; => {:delta 0.6504567155566657, :gamma 0.009884726330871105, :theta -0.020591991134993527, :vega 0.4802358119999822}

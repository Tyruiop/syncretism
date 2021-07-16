(ns syncretism.greeks
  "Following https://www.macroption.com/black-scholes-formula/"
  (:require
   [fastmath.random :as fr]))

;; Risk free interest rate, 
(def rfr 0.0154)
(def cdf-normal (fn [x] (fr/cdf (fr/distribution :normal) x)))

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

(defn calc-premium
  [{opt-type :opt-type X :strike s0 :stock-price eqt :eqt d1 :d1 d2 :d2 t :t q :yield}]
  (let [xert (* X (Math/exp (- (* rfr t))))
        s0eqt (* s0 eqt)]
    (cond (= opt-type "C")
          (- (* s0eqt (cdf-normal d1)) (* xert (cdf-normal d2)))

          (= opt-type "P")
          (- (* xert (cdf-normal (- d2))) (* s0eqt (cdf-normal (- d1)))))))

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

(defn calc-rho
  [{opt-type :opt-type X :strike t :t d2 :d2}]
  (let [xtert (* X t (Math/exp (- (* rfr t))))]
    (cond (= opt-type "C")
          (/ (* xtert (cdf-normal d2)) 100)

          (= opt-type "P")
          (- (/ (* xtert (cdf-normal (- d2))) 100)))))

(defn calculate-greeks
  [{expiration :expiration v :impliedVolatility :as data}]
  (let [t (expiration-years expiration)
        q (calc-annual-yield data)
        d1 (calc-d1 (assoc data :yield q :t t))
        d2 (calc-d2 d1 v t)
        eqt (Math/exp (- (* q t)))

        ;; To avoid recalculating common values
        full-data (assoc data :d1 d1 :d2 d2 :yield q :t t :eqt eqt)]
    (->> [[:delta (calc-delta full-data)]
          [:gamma (calc-gamma full-data)]
          [:theta (calc-theta full-data)]
          [:vega (calc-vega full-data)]
          [:rho (calc-rho full-data)]
          [:premium (calc-premium full-data)]
          [:dividendYield q]
          [:rfr rfr]]
         (filter #(-> % last Double/isNaN not))
         (into {}))))

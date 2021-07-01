(ns datops-compute.yield)

(def month-unit (* 3600 24 30))

(defn calculate-monthly-yield
  "Calculate the yield (premium/strike) and monthly yield of a given option"
  [{:keys [contractsymbol strike ask bid lastprice expiration]}]
  (when (and (not= nil ask) (not= nil bid) (not= nil lastprice)
             (> strike 0) (or (> ask 0) (> lastprice 0)))
    (let [yield (/ (or (when (and (> ask 0) (> bid 0)) (/ (+ ask bid) 2))
                       lastprice)
                   strike)
          now (/ (System/currentTimeMillis) 1000)
          nb-months (/ (- expiration now) month-unit)]
      [(float yield) (float (/ yield nb-months)) contractsymbol])))

;; (calculate-monthly-yield {:strike 100 :ask 3 :expiration (+ (* 3 month-unit) (/ (System/currentTimeMillis) 1000))})
;; => [0.03 0.01 nil]


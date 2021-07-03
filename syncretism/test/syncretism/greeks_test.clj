(ns syncretism.greeks-test
  (:require [clojure.test :refer :all]
            [syncretism.greeks :refer :all]))

(deftest greek-tests
  (let [t1 0.01 t2 2
        {v :impliedVolatility :as ex}
        {:impliedVolatility 0.5 :yield 0.05 :stock-price 100 :strike 95}

        d11 (calc-d1 (assoc ex :t t1))
        d12 (calc-d1 (assoc ex :t t2))
        d21 (calc-d2 d11 v t1)
        d22 (calc-d2 d12 v t2)

        eqt1 (Math/exp (- (* 0.05 t1)))
        eqt2 (Math/exp (- (* 0.05 t2)))

        data1 (assoc ex :eqt eqt1 :d1 d11 :d2 d21 :t t1)
        data2 (assoc ex :eqt eqt2 :d1 d12 :d2 d22 :t t2)]
    (testing "delta should be positive for calls"
      (is (> (calc-delta (assoc data1 :opt-type "C")) 0)))
    (testing "delta should be negative for puts"
      (is (< (calc-delta (assoc data1 :opt-type "P")) 0)))
    (testing "abs(theta) should decrease when expiration is farther"
      (is (< (Math/abs (calc-theta (assoc data2 :opt-type "C")))
             (Math/abs (calc-theta (assoc data1 :opt-type "C"))))))))

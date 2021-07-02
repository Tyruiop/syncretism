(ns datops-compute.timeseries-test
  (:require [clojure.test :refer :all]
            [datops-compute.timeseries :refer :all]))

(deftest test-build-steps
  (testing "checking the three daily slots are correct"
    (let [[_ [open mid close]] (build-steps 1 2)]
      (is (= open (+ (* 9 60 60) (* 30 60))))
      (is (= mid (+ (* 12 60 60) (* 30 60))))
      (is (= close (* 16 60 60)))))
  (testing "we have the correct number of days steps"
    (let [[_ steps] (build-steps 0 (* 3 86400))]
      (is (= 9 (count steps))))))

(deftest test-average
  (testing "if interpolation is correctly done"
    (is (= (average 2 [0 0] [3 2]) [1 4/3]))))

(deftest test-align-option-data-helper
  (let [[_ steps] (build-steps 0 1)
        steps-prev (into [] (map #(- % 10000) steps))
        open+ (+ (* 9 60 60) (* 30 60) 10)
        open- (+ (* 9 60 60) (* 30 60) -10)
        data [[open- 0] [open+ 1]]
        data2 [[-50000 0] [open+ 1]]]
    (testing "should interpolate"
      (is (= (align-option-data-helper [] 0 steps data)
             [[34200 [34200 1/2]] [45000 [34210 1]] [57600 [34210 1]]])))
    (testing "should not interpolate because of day difference in the steps"
      (is (= (align-option-data-helper [] 0 steps data2)
             [[34200 [34210 1]] [45000 [34210 1]] [57600 [34210 1]]])))))

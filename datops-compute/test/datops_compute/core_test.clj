(ns datops-compute.core-test
  (:require [clojure.test :refer :all]
            [datops-compute.core :refer :all]))

(deftest test-parse-args
  (testing "arguments parsing"
    (is (= (parse-args {} ["--yields" "--opts-path" "path/" "--timeseries" "AAPL,CLOV"])
           {:yields true, :opts-path "path/", :timeseries ["AAPL" "CLOV"]}))))

(ns syncretism.time-test
  (:require
   [clojure.test :refer :all]
   [syncretism.time :refer :all])
  (:import
   [java.time ZoneId]))

(deftest test-market-time
  (testing "market-time should report correct market period"
    (let [tz (ZoneId/of "Europe/Prague")
          ts-closed 1625103790000
          ts-pre 1625118190000
          ts-market 1625136190000
          ts-post 1625161390000]
      (is (= (market-time tz ts-closed) "CLOSED"))
      (is (= (market-time tz ts-pre) "PRE"))
      (is (= (market-time tz ts-market) "OPEN"))
      (is (= (market-time tz ts-post) "POST")))))

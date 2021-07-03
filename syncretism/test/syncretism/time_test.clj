(ns syncretism.time-test
  (:require
   [clojure.test :refer :all]
   [syncretism.time :refer :all])
  (:import
   [java.time ZoneId]
   [java.sql Timestamp]))

(def offset
  (-
   (-> 
    (Timestamp. 0)
    (.toLocalDateTime)
    (.atZone (ZoneId/systemDefault))
    java-time/as-map
    :offset-seconds)

   (-> 
    (Timestamp. 0)
    (.toLocalDateTime)
    (.atZone (ZoneId/of "Europe/Prague"))
    java-time/as-map
    :offset-seconds)))

(deftest test-market-time
  (testing "market-time should report correct market period"
    (let [tz (ZoneId/of "Europe/Prague")
          ts-closed (+ 1625103790000 offset)
          ts-pre (+ 1625118190000 offset)
          ts-market (+ 1625136190000 offset)
          ts-post (+ 1625161390000 offset)]
      (is (= (market-time tz ts-closed) "CLOSED"))
      (is (= (market-time tz ts-pre) "PRE"))
      (is (= (market-time tz ts-market) "OPEN"))
      (is (= (market-time tz ts-post) "POST")))))

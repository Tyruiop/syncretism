(ns syncretism.time-test
  (:require
   [clojure.test :refer :all]
   [syncretism.time :refer :all]
   [java-time :as jt])
  (:import
   [java.time ZoneId]
   [java.sql Timestamp]))

(def local-offset
  (-> (System/currentTimeMillis)
      (Timestamp.)
      .toLocalDateTime
      (.atZone (ZoneId/systemDefault))
      jt/as-map
      :offset-seconds
      -))

(defn to-timestamp
  [d]
  (let [{:keys [instant-seconds offset-seconds]}
        (-> d
            (java.time.OffsetDateTime/parse)
            jt/as-map)]
    (* 1000 (+ instant-seconds offset-seconds local-offset))))

(deftest test-market-time
  (testing "market-time should report correct market period"
    (let [tz (ZoneId/of "America/New_York")
          ts-closed (to-timestamp "2021-07-02T01:00:00-04:00")
          ts-pre (to-timestamp "2021-07-02T06:00:00-04:00")
          ts-market (to-timestamp "2021-07-02T12:00:00-04:00")
          ts-post (to-timestamp "2021-07-02T18:00:00-04:00")]
      (is (= (market-time tz ts-closed) "CLOSED"))
      (is (= (market-time tz ts-pre) "PRE"))
      (is (= (market-time tz ts-market) "OPEN"))
      (is (= (market-time tz ts-post) "POST")))))

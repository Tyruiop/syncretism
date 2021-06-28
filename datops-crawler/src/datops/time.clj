(ns datops.time
  (:require
   [java-time :as jt])
  (:import
   [java.sql Timestamp]
   [java.time ZoneId]))

(defn cur-ny-time
  "Utility function to get the current time in NYC."
  []
  (-> (System/currentTimeMillis)
      (Timestamp.)
      .toLocalDateTime
      (.atZone (ZoneId/systemDefault))
      (jt/with-zone-same-instant "America/New_York")
      jt/as-map))

(defn cur-local-time
  "Utility function to get the current time in NYC."
  []
  (-> (System/currentTimeMillis)
      (Timestamp.)
      .toLocalDateTime
      (.atZone (ZoneId/systemDefault))
      jt/as-map))

(defn market-hour?
  "Input ts
  Returns ts [POST|PRE] market time"
  [ts]
  (let [d (-> ts
              (Timestamp.)
              .toLocalDateTime
              (.atZone (ZoneId/systemDefault))
              (jt/with-zone-same-instant "America/New_York")
              jt/as-map)
        {:keys [day-of-week hour-of-day minute-of-hour second-of-day]} d
        mins (+ minute-of-hour (* hour-of-day 60))]
    (not (or (= 6 day-of-week)
             (= 7 day-of-week)
             (< hour-of-day 4)
             (>= hour-of-day 20)))))

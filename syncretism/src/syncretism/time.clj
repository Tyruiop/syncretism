(ns syncretism.time
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

(defn market-time
  "Input ts, in milliseconds
  Returns ts if it is a valid market time, otherwise the next possible market time"
  ([ts] (market-time (ZoneId/systemDefault) ts))
  ([zone ts]
   (let [d (-> ts
               (Timestamp.)
               .toLocalDateTime
               (.atZone zone)
               (jt/with-zone-same-instant "America/New_York")
               jt/as-map)
         {:keys [day-of-week hour-of-day minute-of-hour second-of-day]} d
         mins (+ minute-of-hour (* hour-of-day 60))]
     (cond (= 6 day-of-week)   ;; Saturday
           "CLOSED"
           
           (= 7 day-of-week)   ;; Sunday
           "CLOSED"
           
           (< hour-of-day 4)        ;; Pre-market open (4h)
           "CLOSED"
           
           (>= hour-of-day 20) ;; End market open (20h)
           "CLOSED"

           (and (< hour-of-day 20) (>= hour-of-day 16))
           "POST"

           (and (>= hour-of-day 4)
                (or (< hour-of-day 9)
                    (and (= hour-of-day 9) (< minute-of-hour 30))))
           "PRE"

           (and (<= hour-of-day 16)
                (or (>= hour-of-day 10)
                    (and (= hour-of-day 9) (>= minute-of-hour 30))))
           "OPEN"))))

(defn ts-start-of-day
  "Takes a timestamp, returns the timestamp of the start of that day, NY time.
  Note that ZoneId/systemDefault is used, but the data is crawled in a Europe/Berlin
  timezone server."
  [ts]
  (let [t-map (-> ts
                  (Timestamp.)
                  .toLocalDateTime
                  (.atZone (ZoneId/systemDefault))
                  (jt/with-zone-same-instant "America/New_York")
                  jt/as-map)]
    (- (int (/ ts 1000)) (:second-of-day t-map))))

(defn get-day-of-week
  "Takes a timestamp in seconds and returns the day of the week, assuming NY time."
  [ts]
  (-> ts
      (* 1000)
      (Timestamp.)
      .toLocalDateTime
      (.atZone (ZoneId/of "America/New_York"))
      (jt/with-zone-same-instant "America/New_York")
      jt/as-map
      :day-of-week))

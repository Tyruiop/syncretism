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
             (< mins 270)
             (>= hour-of-day 20)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Functions that will be useful when we try to make the queue smart.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-time
  "Input ts
  Returns ts if it is a valid market time, otherwise the next possible market time"
  [ts]
  (let [d (-> ts
              (* 1000)
              (Timestamp.)
              .toLocalDateTime
              (.atZone (ZoneId/systemDefault))
              (jt/with-zone-same-instant "America/New_York")
              jt/as-map)
        {:keys [day-of-week hour-of-day minute-of-hour second-of-day]} d
        mins (+ minute-of-hour (* hour-of-day 60))]
    (cond (= 6 day-of-week)   ;; Saturday
          (+ (- ts second-of-day) (* 2 86400) (* 270 60))
          
          (= 7 day-of-week)   ;; Sunday
          (+ (- ts second-of-day) 86400 (* 270 60))
          
          (< mins 270)        ;; Pre-market open (4h30)
          (+ (- ts second-of-day) (* 270 60))
          
          (>= hour-of-day 20) ;; End market open (20h)
          (+ (- ts second-of-day) 86400 (* 270 60))

          :else ts)))

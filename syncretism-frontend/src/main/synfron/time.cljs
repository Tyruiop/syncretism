(ns synfron.time)

(defn from-ts
  [ts]
  (-> ts
      (* 1000)
      (js/Date.)
      (.toLocaleString "en-US")
      ))

(defn cur-ny-time
  []
  (-> (js/Date.)
      (.toLocaleString "en-US" (clj->js {:timeZone "America/New_York"}))
      js/Date.parse
      (/ 1000)
      int))

(defn cur-gmt-time
  []
  (-> (js/Date.)
      (.toLocaleString "en-US" (clj->js {:timeZone "GMT"}))
      js/Date.parse
      (/ 1000)
      int))

(defn cur-local-time
  []
  (-> (js/Date.)
      (.toLocaleString "en-US")
      js/Date.parse
      (/ 1000)
      int))

(def offset (- (cur-gmt-time) (cur-ny-time)))
(def offset-exp (- (cur-gmt-time) (cur-local-time)))

(defn s-to-h-min
  "Takes seconds and convert them to h:min:s"
  [s]
  (let [hs (.floor js/Math (/ s 3600))
        mins (.floor js/Math (/ (mod s 3600) 60))]
    (cond (> hs 0) (str hs "h" (if (< mins 10) (str "0" mins) mins) "min")
          (> mins 0) (str mins "min")
          :else "< 1 min")))

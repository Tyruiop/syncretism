(ns syncretism-client.filters-def
  (:import
   [imgui.type ImBoolean ImString ImFloat]))

(def filters
  {"diff" 
   {:title "Strike to Price difference (in %)"
    :type :min-max
    :min (new ImFloat 0.0)
    :max (new ImFloat 1000.0)
    :descr "Control how far from the money the options should be."}

   "active"
   {:title "Active"
    :type :boolean
    :value  (new ImBoolean false)
    :descr "Restrict search to options for which V > 0, Bid > 0, and OI > 0"}})

(defmulti process-filter (fn [_ f-data] (:type f-data)))

(defmethod process-filter :min-max
  [f-id {min-v :min max-v :max}]
  {(str "min-" f-id) (.get min-v)
   (str "max-" f-id) (.get max-v)})

(defmethod process-filter :boolean
  [f-id {:keys [value]}]
  {(keyword f-id) (.get value)})

(defn convert
  [filters]
  (reduce
   (fn [acc [f-id f-data]]
     (merge acc (process-filter f-id f-data)))
   {}
   filters))

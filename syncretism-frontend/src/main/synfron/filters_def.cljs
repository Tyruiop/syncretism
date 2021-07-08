(ns synfron.filters-def)

(def all-filters
  [{:title "Option types"
    :type :checkboxes
    :entries
    [{:name "ItM"
      :descr "Filter by in the money options."
      :id "itm"}
     {:name "OtM"
      :descr "Filter by out of the money options."
      :id "otm"}
     {:name "Calls"
      :id "calls"}
     {:name "Puts"
      :id "puts"}]}

   {:title "Strike to Price difference"
    :type :min-max
    :descr nil
    :id "diff"}

   {:title "Option Premium"
    :type :min-max
    :descr nil
    :id "price"}])

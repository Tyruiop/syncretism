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

   {:title "Strike to Price difference (in %)"
    :type :min-max
    :descr "To control how far from the money the options should be."
    :id "diff"}

   {:title "Bid-Ask spread"
    :type :min-max
    :descr nil
    :id "ask-bid"}

   {:title "Days to expiration date"
    :type :min-max
    :descr nil
    :id "exp"}

   {:title "Implied volatility"
    :type :min-max
    :descr nil
    :id "iv"}

   {:title "Option Premium"
    :type :min-max
    :descr nil
    :id "price"}

   {:title "Option Price / Stock Price ratio"
    :type :min-max
    :descr nil
    :id "sto"}

   {:title "Yield"
    :type :min-max
    :descr "The returns of the premium compared to the strike."
    :id "yield"}

   {:title "Monthly yield"
    :type :min-max
    :descr nil
    :id "myield"}

   {:title "Greek: Delta (δ)"
    :type :min-max
    :descr "Speed of the premium's change compared to the underlying stock's movement."
    :id "delta"}

   {:title "Greek: Gamma (γ)"
    :type :min-max
    :descr "Speed of δ's change."
    :id "gamma"}
   
   {:title "Greek: Theta (θ)"
    :type :min-max
    :descr "Impact of the time to expiration on the premium."
    :id "theta"}
   
   {:title "Greek: Vega (ν)"
    :type :min-max
    :descr "Impact of IV on the premium."
    :id "vega"}

   {:title "Market Capitalization (USD billions)"
    :type :min-max
    :descr nil
    :id "cap"}

   {:title "Security type"
    :type :checkboxes
    :entries
    [{:name "Stock"
      :descr "Search regular stocks."
      :id "stock"}
     {:name "ETF"
      :descr "Search exchange traded funds."
      :id "etf"}]}])

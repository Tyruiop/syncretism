(ns synfron.filters-def)

(def all-filters
  [{:title "Miscellaneous"
    :type :misc
    :descr [:p [:strong "Active"]
            " means only options for which ask, bid, OI, and V are > 0. "]
    :entries
    [{:name "Tickers: "
      :type :text
      :placeholder "e.g. AMC, GME"
      :id "tickers"}
     {:name "Exclude "
      :type :checkbox
      :id "exclude"}
     {:name "Order by "
      :type :select
      :id "order-by"
      :options [["Expiration ↓" "e_desc"]
                ["Expiration ↑" "e_asc"]
                ["Implied Volatility ↓" "iv_desc"]
                ["Implied Volatility ↑" "iv_asc"]
                ["Premium ↓" "lp_desc"]
                ["Premium ↑" "lp_asc"]
                ["Strike ↓" "s_desc"]
                ["Strike ↑" "s_asc"]
                ["Symbol ↓" "t_desc"]
                ["Symbol ↑" "t_asc"]
                ["Open Interest ↓" "oi_desc"]
                ["Open Interest ↑" "oi_asc"]
                ["Volume ↓" "v_desc"]
                ["Volume ↑" "v_asc"]
                ["Stock Price ↓" "md_desc"]
                ["Stock Price ↑" "md_asc"]]}
     {:name "Active"
      :type :checkbox
      :descr ""
      :id "active"}]}

   {:title "Option types"
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

   {:title "Security type"
    :type :checkboxes
    :entries
    [{:name "Stock"
      :descr "Search regular stocks."
      :id "stock"}
     {:name "ETF"
      :descr "Search exchange traded funds."
      :id "etf"}]}

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

   {:title "Option Premium"
    :type :min-max-with-var
    :descr nil
    :id "price"}

   {:title "Implied volatility"
    :type :min-max-with-var
    :descr nil
    :id "iv"}

   {:title "Open Interest"
    :type :min-max-with-var
    :descr nil
    :id "oi"}

   {:title "Volume"
    :type :min-max-with-var
    :descr nil
    :id "volume"}

   {:title "Volume / OI ratio"
    :type :min-max
    :descr nil
    :id "voi"}

   {:title "Option Price / Stock Price ratio"
    :type :min-max
    :descr nil
    :id "sto"}

   {:title "Strike"
    :type :min-max
    :descr "Strike price range"
    :id "strike"}

   {:title "Stock Price"
    :type :min-max
    :descr "Stock price range"
    :id "stock"}

   {:title "Yield"
    :type :min-max
    :descr "The returns of the premium compared to the strike."
    :id "yield"}

   {:title "Monthly yield"
    :type :min-max
    :descr nil
    :id "myield"}

   {:title "Greek: Delta (δ)"
    :type :min-max-with-var
    :descr "Speed of the premium's change compared to the underlying stock's movement."
    :id "delta"}

   {:title "Greek: Gamma (γ)"
    :type :min-max-with-var
    :descr "Speed of δ's change."
    :id "gamma"}
   
   {:title "Greek: Theta (θ)"
    :type :min-max-with-var
    :descr "Impact of the time to expiration on the premium."
    :id "theta"}
   
   {:title "Greek: Vega (ν)"
    :type :min-max-with-var
    :descr "Impact of IV on the premium."
    :id "vega"}

   {:title "Greek: Rho (ρ)"
    :type :min-max-with-var
    :descr "Impact of the risk free interest rate on the premium."
    :id "rho"}

   {:title "Market Capitalization (USD billions)"
    :type :min-max
    :descr nil
    :id "cap"}])

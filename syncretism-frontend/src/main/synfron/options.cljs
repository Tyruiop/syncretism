(ns synfron.options
  (:require [synfron.state :as state]))

(def columns-w-names
  [[:contractSymbol "Contract Symbol" "CS"]
   [:symbol "Symbol" "S"]
   [:optType "Type" "T"]
   [:strike "Strike" "Str"]
   [:expiration "Expiration" "Exp"]
   [:lastTradeDate "Last Trade Date" "LTD"]
   [:impliedVolatility "Implied Volatility" "IV"]
   [:bid "Bid" "B"]
   [:ask "Ask" "A"]
   [:lastPrice "Last Price" "LP"]
   [:volume "Volume" "V"]
   [:openInterest "Open Interest" "OI"]
   [:yield "Yield" "Y"]
   [:monthlyyield "Monthly Yield" "MY"]
   [:inthemoney "In the Money" "ItM"]
   [:pchange "Price Change" "PC"]
   [:regularMarketPrice "Market Price" "MP"]
   [:regularMarketDayLow "Market Day Low" "MDL"]
   [:regularMarketDayHigh "Market Day High" "MDH"]
   [:delta "Delta" "δ"]
   [:gamma "Gamma" "γ"]
   [:theta "Theta" "θ"]
   [:vega "Vega" "ν"]
   [:quotetype "Quote Type" "QT"]
   [:lastCrawl "Last Updated" "LU"]])

(defn render
  []
  [:div
   (doall
    (map
     #(do [:p (str %)])
     (get-in @state/app-state [:options :data :options])))])

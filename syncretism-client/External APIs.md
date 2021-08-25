#External API:s

##TD Ameritrade
### Get Option Ladders
####Example Use:
`curl -X GET https://api.tdameritrade.com/v1/marketdata//chains?apikey=ENTERKEY&symbol=AAPL`

Returns every option for the ticker `AAPL`.

###Parameters

* `apikey [string]`: Enter the API key **Requirement**
* `symbol [string]`: One ticker for the option ladder **Requirement**
* `contractType [string]`: Type of contracts to return in the ladder. Can be CALL, PUT, or ALL. **default:ALL**
* `strikeCount [int]`: The number of strikes to return above and below the at-the-money price
* `includeQuotes [true|false]`: Include quotes for options in the option chain. Can be TRUE or FALSE. **default:FALSE**
* `stratedgy [string]`: Passing a value returns a Strategy Chain. Possible values are SINGLE, ANALYTICAL (allows use of the volatility, underlyingPrice, interestRate, and daysToExpiration params to calculate theoretical values), COVERED, VERTICAL, CALENDAR, STRANGLE, STRADDLE, BUTTERFLY, CONDOR, DIAGONAL, COLLAR, or ROLL. **Default:SINGLE**
* `interval []`: Strike interval for spread strategy chains (see strategy param)
* `strike [int]`: Provide a strike price to return options only at that strike price.
* `range [str]`: Returns options for the given range. Possible values are:
ITM (In-the-money),NTM (Near-the-money), OTM (Out-of-the-money), SAK (Strikes Above Market), SBK (Strikes Below Market), SNK (Strikes Near Market) and ALL (All Strikes) **Default:ALL**
* `fromDate [date]`: Only return expirations after this date. For strategies, expiration refers to the nearest term expiration in the strategy. Valid ISO-8601 formats are: yyyy-MM-dd and yyyy-MM-dd'T'HH:mm:ssz.
* `toDate [date]`: Only return expirations before this date. For strategies, expiration refers to the nearest term expiration in the strategy. Valid ISO-8601 formats are: yyyy-MM-dd and yyyy-MM-dd'T'HH:mm:ssz.
* `volatility [float]`: Volatility to use in calculations. Applies only to ANALYTICAL strategy chains (see strategy param).
* `underlyingPrice [float]`: Underlying price to use in calculations. Applies only to ANALYTICAL strategy chains (see strategy param).
* `interestRate [float]`: Interest rate to use in calculations. Applies only to ANALYTICAL strategy chains (see strategy param).
* `daysToExpiration [float]`: Days to expiration to use in calculations. Applies only to ANALYTICAL strategy chains (see strategy param).
* `expMonth [str]`: Return only options expiring in the specified month. Month is given in the three character format. Example: JAN **Default:ALL**
* `optionType [str]`: Type of contracts to return. Possible values are: S (Standard contracts), NS (Non-standard contracts) and ALL (All contracts) **Default:ALL**

For more information visit: https://developer.tdameritrade.com
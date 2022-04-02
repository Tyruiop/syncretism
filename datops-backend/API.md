# API

## Get a specific contract

### Example use:

`curl -X GET https://api.syncretism.io/ops/AAPL210716C00145000`

Will return all the latest information about `AAPL210716C00145000`.

## Search for options

### Example use:

`curl -X GET https://api.syncretism.io/ops  -H "Content-Type: application/json" -d '{"max-price": 0.5, "puts": true}'`

or

`curl -X POST https://api.syncretism.io/ops  -H "Content-Type: application/json" -d '{"max-price": 0.5, "puts": true}'`

the difference between `POST` and `GET` is that `GET` will only return options data, while the `POST` method will also return **the underlying's stock quotes and catalysts**.

`{"options": […], "quotes": […], "catalysts": […]}`.

### Parameters

* `tickers [string]`: a list of space or comma separated tickers to restrict or exclude them from the query.

* `exclude [true|false]`: if true, then `tickers` are excluded. If false, the search is restricted to these tickers.

* `min-diff [int]`: minimum difference in percentage between strike and stock price.

* `max-diff [int]`: maximum difference in percentage between strike and stock price.

* `itm [bool]`: select in the money options, **default: true**.

* `otm [bool]`: select out of the money options, **default: true**.

* `min-breakeven [float]`: minimum breakeven value in %. i.e. the required change in the underlying security for the 
  option to be worth something given its current premium.

* `max-breakeven [float]`: maximum breakeven value in %.

* `min-ask-bid [float]`: minimum spread between bid and ask.

* `max-ask-bid [float]`: maximum spread between bid and ask.

* `min-exp [int]`: minimum days until expiration.

* `max-exp [int]`: maximum days until expiration.

* `min-iv [float]`: minimum implied volatility.

* `max-iv [float]`: maximum implied volatility.

* `min-oi [float]`: minimum open interest.

* `max-oi [float]`: maximum open interest.

* `min-volume [float]`: minimum volume.

* `max-volume [float]`: maximum volume.

* `min-voi [float]`: minimum volume / oi ratio.

* `max-voi [float]`: maximum volume / oi ratio.

* `min-price [float]`: minimum option premium.

* `max-price [float]`: maximum option premium.

* `calls [true|false]`: select call options **default: true**.

* `puts [true|false]`: select put options **default: true**.

* `stock [true|false]`: select normal stocks **default: true**.

* `etf [true|false]`: select etf options **default: true**.

* `min-sto [float]`: minimum option price / stock price ratio.

* `max-sto [float]`: maximum option price / stock price ratio.

* `min-strike [float]`: minimum option strike.

* `max-strike [float]`: maximum option strike.

* `min-stock [float]`: minimum underlying stock's price.

* `max-stock [float]`: maximum underlying stock's price.

* `min-yield [float]`: minimum premium / strike price ratio.

* `max-yield [float]`: maximum premium / strike price ratio.

* `min-myield [float]`: minimum yield per month until expiration date.

* `max-myield [float]`: maximum yield per month until expiration date.

* `min-delta [float]`: minimum delta greek.

* `max-delta [float]`: maximum delta greek.

* `min-gamma [float]`: minimum gamma greek.

* `max-gamma [float]`: maximum gamma greek.

* `min-theta [float]`: minimum theta greek.

* `max-theta [float]`: maximum theta greek.

* `min-vega [float]`: minimum vega greek.

* `max-vega [float]`: maximum vega greek.

* `min-cap [float]`: minimum market capitalization (in billions USD).

* `max-cap [float]`: maximum market capitalization (in billions USD).

* `order-by [default: e_desc]`: how to order results, possible values:
  * `e_desc`, `e_asc`: expiration, descending / ascending.
  * `iv_desc`, `iv_asc`: implied volatility, descending / ascending.
  * `lp_desc`, `lp_asc`: lastprice, descending / ascending.
  * `md_desc`, `md_asc`: current stock price, descending / ascending.
  
* `limit [int]`: number of results (max 50).

* `offset [int]`: for pagination (e.g. if you got 50 result, set offset to 50 for the next ones).

* `active [true|false]`: if set to true, restricts to options for which volume, open interest, ask, and bid are all `> 0`.

On top off all these, you can also filter by deviation from the 20 days or 100 days
average with the following variables:

* `min-price-20d` `max-price-20d`
* `min-iv-20d` `max-iv-20d`
* `min-oi-20d` `max-oi-20d`
* `min-volume-20d` `max-volume-20d`
* `min-delta-20d` `max-delta-20d`
* `min-gamma-20d` `max-gamma-20d`
* `min-theta-20d` `max-theta-20d`
* `min-vega-20d` `max-vega-20d`
* `min-rho-20d` `max-rho-20d`

* `min-price-100d` `max-price-100d`
* `min-iv-100d` `max-iv-100d`
* `min-oi-100d` `max-oi-100d`
* `min-volume-100d` `max-volume-100d`
* `min-delta-100d` `max-delta-100d`
* `min-gamma-100d` `max-gamma-100d`
* `min-theta-100d` `max-theta-100d`
* `min-vega-100d` `max-vega-100d`
* `min-rho-100d` `max-rho-100d`

For example, setting `min-volume-20d = 0.3` will filter by
`abs(volume - vol20d)/volume >= 0.3`.

## Ladder request

### Example use:

`curl -X GET https://api.syncretism.io/ops/ladder/PAYO/C/1626393600`

i.e. `ops/ladder/TICKER/OPT-TYPE/EXPIRATION`

note that the expiration date must be unique.

### Parameters

All parameters are required.

* `ticker [string]`: target ticker.

* `expiration [int]`: target expiration date, timestamp in seconds.

* `opt-type [string]`: either `C` (call) or `P` (put).

## Option chain

Similar to ladder request, but only returns the contract symbol, sorted by calls `C` and puts `P`.

### Example use:

`curl -X GET https://api.syncretism.io/ops/chain/PAYO/1626393600`

### Parameters

All parameters are required.

* `ticker [string]`: target ticker.

* `expiration [int]`: target expiration date, timestamp in seconds.

## Historical data

### Example use:

Endpoint is `/ops/historical` and is called by passing an option name as parameter, e.g.:

`curl -X GET https://api.syncretism.io/ops/historical/PYPL210820P00280000`

## Get all expiration dates for a given symbol

### Example use:

`curl -X GET https://api.syncretism.io/ops/expirations/AAPL`

Will return all the valid expiration dates for `AAPL` (to be passed to the
ladder API call for example).

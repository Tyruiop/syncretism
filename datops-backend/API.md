# API

## Example use:

`curl -X GET https://api.syncretism.io/ops  -H "Content-Type: application/json" -d '{"max-price": 0.5, "puts": true}'`

## Parameters

* `tickers [string]`: a list of space or comma separated tickers to restrict or exclude them from the query.

* `exclude [true|false]`: if true, then `tickers` are excluded. If false, the search is restricted to these tickers.

* `min-diff [int]`: minimum difference in percentage between strike and stock price.

* `max-diff [int]`: maximum difference in percentage between strike and stock price.

* `min-ask-bid [float]`: minimum spread between bid and ask.

* `max-ask-bid [float]`: maximum spread between bid and ask.

* `min-exp [int]`: minimum days until expiration.

* `max-exp [int]`: maximum days until expiration.

* `min-price [float]`: minimum option premium.

* `max-price [float]`: maximum option premium.

* `calls [true|false]`: select call options.

* `puts [true|false]`: select put options.

* `stock [true|false]`: select normal stocks.

* `etf [true|false]`: select etf options.

* `min-sto [float]`: minimum option price / stock price ratio.

* `max-sto [float]`: maximum option price / stock price ratio.

* ~~`min-pso [float]`: deprecated~~ (see `min-yield`)

* ~~`max-pso [float]`: deprecated~~ (see `max-yield`)

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

* `active [true|false]`: if set to true, restricts to options for which volume, open interest, ask, and bid are all `> 0`.

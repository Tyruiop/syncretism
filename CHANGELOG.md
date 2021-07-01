# Week 26, 2021

## Filters
* Added filters over greeks (delta/gamma/theta/vega)

## UI
* sticky headers
* Displays greeks
* Ability to show spread (closest option).
* Show options in/out of the money with background color

## Backend
* Crawler calculates greeks as it goes!
* Ability to created aligned time series based on the collected historical data.

## API
* New endpoint: https://api.syncretism.io/historical/
* Ability to return option ladder for a given (ticker, opt type, expiration date) triplet.

## Misc
* Creation of `labs` folder that shows how to consume the data and how to display it.

# Week 25, 2021

## Filters
* Added filter by `Option Price / Stock Price`
* Added filter by market cap
* Added filter by only ETF, only Stocks or both
* Added filter by expiration date
* Added filter by Premium / Strike
* Added filter by Active (V > 0, OI > 0, A > 0, B > 0) options.
* Added moneyness filter (ItM/OtM checkboxes)
* Added yield & monthly yield filters

## UI
* Revamped filters, should be less clutered now
* Infobox for some filters
* Displays catalysts (earnings/dividends).

## Backend
* Crawler gets fundamentals to db
* Ability to request catalysts from FE.

## API
* Endpoint available at https://api.syncretism.io/ops
* Documentation is available at https://ops.syncretism.io/api.html

## Misc
* If `ask` column is 0, try to use `lastprice` where applicable in queries.
* Crawler now also populates `regularmarketprice` column
* `regularmarketprice` column used in filters in lieu of daily high/low
* BE/FE ability to exchange current market status.

## Bugs
* Fix order by bug on the frontend side.
* The premium filter would resort to the `last trade price` over the `ask` column even when `ask` was  not at 0, `ask` is not properly given priority.

# Week 24, 2021

## Filters
* Added ask-bid spread filter
* Added min price filter for theta gang

## Misc
* Added links to Yahoo finance in option's extra info.
* Potentially solves the timezone problem that surfaces for some people on the frontend.

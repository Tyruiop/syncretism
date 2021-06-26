# Week 25, 2021

## Filters
* Added filter by `Option Price / Stock Price`
* Added filter by market cap
* Added filter by only ETF, only Stocks or both
* Added filter by expiration date
* Added filter by Premium / Strike
* Added filter by Active (V > 0, OI > 0, A > 0, B > 0) options.
* Added moneyness filter (ItM/OtM checkboxes)

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

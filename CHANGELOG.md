# Week 30, 2021

## Filters
* Added Volume/Open Interest ratio filter

## UI
* Fixed bug where searching for more option was resetting the filters.
* Ability to sort results by column.

## Backend
* Compute 20 days and 100 days average for volume, OI, IV, premium, bid, and all the greeks.
* New API call: `/ops/expirations/SYMBOL` to list all possible expirations.
* New API call: `/ops/chain/SYMBOL/EXPIRATION` to list option chain.

# Week 29, 2021

## Filters
* Added Volume filter
* Added Open Interest filter

## UI
* Added order by `symbol` and order by `strike` to search.
* Added order by `volume` and order by `open interest` to search.

## Backend
* Added order by `symbol` and order by `strike` to search.

# Week 28, 2021

## Filters
* Added strike price filter
* Added underlying stock's price filter

## UI
* Work on making the UI more smartphone friendly.
* Fix data persistence bugs (w/r to plot display).

## Syncretism lib
* Added theoretical premium calculation
* Added rho calculation
* More complete data saved in db

## Backend
* Can trigger day by day complete historical timeseries computation

## Syncretism Client
* Preliminary work.
* Design of the program state.

## Misc
* Adding rho & premium to historical data (in progress).

# Week 27, 2021

## UI
* Complete UI revamp
* Saving option data locally
* Ability to track options
* Plotting historical option data for tracked options.
* Ability to save/load custom filters
* Added donation link (`Buy me a coffee`)
* Uses backend API instead of custom endpoint.
* Ability to search for filters instead of having to go through them all.

## API
* New endpoint: `/ops/CONTRACTNAME` to get data about a specific contract.
* Renamed: `/historical` into `/ops/historical` for consistency.
* `/ops` exists both as `GET` and `POST`, `POST` returns more complete information.

## Misc
* Preliminary work on backtesting & prediction projects!
* See https://github.com/Tyruiop/syncretism-labs/blob/main/labs/data_exploration.ipynb

# Week 26, 2021

## Filters
* Added filters over greeks (delta/gamma/theta/vega)
* Added filter over IV

## UI
* sticky headers
* Displays greeks
* Ability to show spread (closest option).
* Show options in/out of the money with background color
* Pagination of results

## Backend
* Crawler calculates greeks as it goes!
* Ability to created aligned time series based on the collected historical data.
* Pagination of results

## API
* New endpoint: https://api.syncretism.io/historical/, see https://github.com/Tyruiop/syncretism-labs/blob/main/labs/comparing_two_options.ipynb for an example.
* Ability to return option ladder for a given (ticker, opt type, expiration date) triplet.

## Misc
* Creation of `syncertism-labs` repository that shows how to consume data and how to display it.
* Creation of `syncretism` library, containing all the calculation/"smart" reusable bits.
* Addition of tests.
* Added CI to the repo.
* Massive SQL vulnerability fixed.

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

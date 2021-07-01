# Syncretism

[Ops.Syncretism](https://ops.syncretism.io) is an open source options search engine based on [Yahoo! Finance](https://finance.yahoo.com) market data.

Dependencies are [Clojure](https://clojure.org/), [Leiningen](https://leiningen.org/), [NodeJS](https://nodejs.org/en/), and [MariaDB](https://mariadb.org/).
We try to minimize dependencies to make it easy to install and run locally.

Join us on [Discord](https://discord.gg/qBWD5Sus3d) for any question you might have. 
Use [Github Issues](https://github.com/Tyruiop/syncretism/issues) for bug reports & feature requests.

## Crawler `/datops-crawler`

This crawlers targets **Yahoo Finance** market data to extract options related information.

### Running it

Dependency on MariaDB or MySQL. Set parameters in `datops-crawler/resources/db.edn` as illustrated [Here](https://github.com/clojure/java.jdbc/).

For example, if your user is `datops` and the created database is `opts`, the `db.edn` becomes:
```
{:dbtype "mysql"
 :dbname "opts"
 :user "datops"
 :password "12345password"
 :rewriteBatchedStatements true}
```

You should also set `datops-crawler/resources/config.edn` as follows:
```
{;; Should we crawl out of market hours
 :force-crawl false
 ;; Do we log debug info
 :debug false
 ;; How often to we save & reorder the queue & the proxies (in seconds)
 :t-reorder 60
 ;; How often do we fully clean the queue (remove expired options & gather new symbols)
 :t-clean-queue 86400
 ;; Number of endpoints to use.
 :nb-endpoints 2
 ;; Max Batch size when writing to db
 :batch-size 10000
 ;; Where to save the options data
 :save-path "options"}
```

Initialize the db with `lein run --init`.

Run it with `lein run`

Logs are written in `./opts-crawler.log`

### Data generated

The crawler uses a scheduler to make sure that any `[Ticker, Date]` pair that failed to be
crawled will be given priority in the queue.

With the above configuration file, the data is stored in the folder `options`.
The crawler creates one folder per day, and one gzipped file per symbol.

### Live DB

In `options` db:
- `live` table contains some essential information about the latest measured state of all options. See `live-table-def` in `db_init.clj`.
- `live_quote` contains essential information about the underlying stock. See `live-quote-table-def` in the same file.
- `fundamentals` contains all the fundamental data available for a given ticker (e.g. earnings, dividends etc...).

## Backend `/datops-backend`

### Running it

As with the *crawler*, there is a dependency on MariaDB or MySQL. Set parameters in `datops-backend/resources/db.edn` as illustrated [Here](https://github.com/clojure/java.jdbc/).

For example:
```
{:dbtype "mysql"
 :dbname "opts"
 :user "datops"
 :password "12345password"
 :rewriteBatchedStatements true}
```

Run it with `lein ring server-headless`, will default to port `3000`.

If you want to run it on a different port, simply do
`lein ring server-headless 3001`
to run it on the port 3001 for example.

### Api documentation

[Avaiable here](datops-backend/API.md).


## Frontend `/datops-frontend`

Depends on `NodeJS`.

Change the server address in `src/live_opts/core.cljs` to point to your local `datops-backend`, in the above example
it would become `"http://localhost:3000/"`.

Build with
```
npm install
./node_modules/.bin/shadow-cljs release :frontend
```

Copy & paste the `/public` folder wherever your local http server is.

## Data processing `/datops-compute`

Very early stage, software to do post processing on the crawled data. For the moment:
* can be used to calculate the yield & monthly yield of all non expired options.
* can be used to create aligned time series from the crawled data.

## Labs `/labs`

Quick examples / experiments regarding what can be done with the data.

# Licence

Syncretism is licensed under the GNU Affero General Public License 3 or any later version at your choice.

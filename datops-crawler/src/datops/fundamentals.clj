(ns datops.fundamentals
  (:require
   [taoensso.timbre :as timbre :refer [info warn error]]
   [clojure.data.json :as json]
   [clj-http.client :as http]
   [datops.db :as odb]
   [datops.time :refer [market-hour?]]
   [datops.shared :refer [symbols]]))

(defn format-address
  [ticker]
  (str
   "https://query1.finance.yahoo.com/v10/finance/quoteSummary/"
   ticker
   "?modules=assetProfile,summaryProfile,summaryDetail,esgScores,price,incomeStatementHistory,incomeStatementHistoryQuarterly,balanceSheetHistory,balanceSheetHistoryQuarterly,cashflowStatementHistory,cashflowStatementHistoryQuarterly,defaultKeyStatistics,financialData,calendarEvents,secFilings,recommendationTrend,upgradeDowngradeHistory,institutionOwnership,fundOwnership,majorDirectHolders,majorHoldersBreakdown,insiderTransactions,insiderHolders,netSharePurchaseActivity,earnings,earningsHistory,earningsTrend,industryTrend,indexTrend,sectorTrend"))

(defn get-ticker-data
  [ticker]
  (let [base-params {:accept :json :socket-timeout 10000 :connection-timeout 10000
                     :retry-handler (fn [_ex try-count _http-context]
                                      (if (> try-count 4) false true))}]
    (-> ticker
        format-address
        (http/get base-params)
        :body
        json/read-str
        first
        last
        (get "result")
        first)))

(defn process-data
  [[ticker & r]]
  (when ticker
    (try
      (let [data (get-ticker-data ticker)]
        (odb/insert-or-update-fundamentals ticker (json/write-str data)))
      (catch Exception e (warn (str "Error with ticker " ticker))))
    (Thread/sleep 1800)
    (recur r)))

(defn crawler
  []
  (when (not (market-hour? (System/currentTimeMillis)))
    (let [tickers (keys @symbols)]
      (info "Gather fundamental data.")
      (process-data tickers)
      (info (str "Fundamental data acquire for " (count tickers)))))
  (Thread/sleep (* 3 60 60 1000)) ;; wait 3h, so we don't do it so often.
  (recur))

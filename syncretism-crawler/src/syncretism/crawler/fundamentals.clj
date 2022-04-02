(ns syncretism.crawler.fundamentals
  (:require
   [taoensso.timbre :as timbre :refer [info warn error]]
   [clojure.data.json :as json]
   [clj-http.client :as http]
   [syncretism.crawler.db :as odb]
   [syncretism.crawler.shared :refer [symbols state]]
   [syncretism.time :refer [market-time]]))

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

(defn crawler
  ([] (crawler [] (keys @symbols)))
  ([seen [ticker & r :as tickers]]
   (case (:fundamentals-status @state)
     :running
     (if ticker
       (do
         ;; (println (format "Gathering fundamentals for %s" ticker))
         (try
           (let [data (get-ticker-data ticker)]
             (odb/insert-or-update-fundamentals ticker (json/write-str data)))
           (catch Exception e (warn (str "Error with ticker " ticker))))
         (Thread/sleep 1800)
         (recur (conj seen ticker) r))
       (recur [] (keys @symbols)))

     :paused
     (do
       (Thread/sleep 1000)
       (recur seen tickers))

     :terminate
     (do
       (info "Terminating fundamentals crawler.")
       :done))))


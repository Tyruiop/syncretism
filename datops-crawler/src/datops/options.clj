(ns datops.options
  (:require
   [taoensso.timbre :as timbre :refer [info warn error]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.data.json :as json]
   [clojure.set :as set]
   [clj-http.client :as http]
   [datops.db :as odb]
   [datops.shared :refer [config symbols update-tickers-dict]]
   [datops.endpoints :refer [all-endpoints available-endpoints
                             register-failure sort-endpoints]]
   [syncretism.time :refer [cur-ny-time market-time]]
   [syncretism.greeks :as gks])
  (:gen-class))

(def iter (atom (long 1)))

(defn get-ticker-data
  "Given an endpoint and possibly a proxy, extract a given ticker's option
  data on a given date (can be nil)."
  [{:keys [endpoint proxy]} ticker date]
  (let [addr (if date
               (str endpoint ticker "?date=" date)
               (str endpoint ticker))
        base-params {:accept :json :socket-timeout 10000 :connection-timeout 10000
                     :retry-handler (fn [_ex try-count _http-context] (if (> try-count 4) false true))}
        params (if proxy
                 (merge
                  base-params
                  {:proxy-host (first proxy) :proxy-port (second proxy)})
                 base-params)]
    (-> (http/get addr params)
        :body
        (json/read-str :key-fn keyword)
        (get-in [:optionChain :result])
        first
        (assoc :status :success))))

(defn gz-write-line
  "Append data to gziped target"
  [target content]
  (with-open [w (-> target
                    (clojure.java.io/output-stream :append true)
                    java.util.zip.GZIPOutputStream.
                    clojure.java.io/writer)]
    (binding [*out* w]
      (println content))))

;; CRAWLER
;; -------
;;
;; REQUEST QUEUE
;; -------------
;; (count @symbols)
;; NYSE + NASDAQ have about 7400 listed symbols with our current filters.
;; Queue reached ~ 50 000 entries, and found 650 000 different options valid at a given time.
;;
;; We can make 48000×2 requests to Yahoo Finance, so 96000 requests.
;; We start with a simple queue that will query Yahoo every 1s.
;; (alternating query endpoint)
;; Once a day, clean the queue and the symbols list (mainly, remove all options that
;; have expired)
;;
;; Query a (symbol, date) pair, add any never seen before expiration date to the queue,
;; and append a new call for the on just seen.
;;
;; STORING DATA
;; ------------
;; One file per symbol per day.
;; One line per option, line is the merge of
;;    * `quote`             → info related to the underlying stock
;;    * `opt`               → info related to the option
;;    * opt-type, cur-time  → extra fields to complete all info
;; Filename should be TYPE-TICKER-STRIKEPRICE-DATE, e.g. C-AMZN-100-1600000000
;; Note, all times should be NY time.
;;
;; Eventually we will want to add more fields (e.g. sentiment analysis) to each record.

;; List[[ticker date nb-views]]
(def queue (atom []))

(defn clean-queue
  "Remove all expired options from the queue, not to crawl them anymore"
  [old deleted new]
  (let [cur-time (cur-ny-time)
        limit-expiration (+ (:instant-seconds cur-time) (:offset-seconds cur-time))
        new-entries (into [] (map #(do [% nil 0]) new))]
    (->> old
         (filter (fn [[ticker date _]]
                   (and (nil? (deleted ticker))
                        (or (nil? date)
                            (> date limit-expiration)))))
         (into new-entries))))

(defn clean-queue-inside-loop
  []
  (info "Updating queue")
  (let [{:keys [deleted]} (update-tickers-dict)
        all-symbs (into #{} (keys @symbols))
        queue-symbs (into #{} (map first @queue))
        new (set/difference all-symbs queue-symbs)]
    (swap! queue clean-queue deleted new)
    (info (str "New symbols found " new))
    (info (str "Symbols removed " deleted))
    (info (str "Updated queue size: " (count @queue)))
    ;; Know which dates we are looking at already
    (doseq [[symb _] @queue]
      (swap! symbols #(assoc-in % [symb :exp-dates] #{})))
    (doseq [[symb date] @queue]
      (swap! symbols #(update-in % [symb :exp-dates] conj date)))
    (info (str "Cleaning done"))))

(defn clean-queue-loop
  "Once a day, clean the queue, queue is short enough that it doesn't really matter when.
  t is in seconds"
  [t]
  (while true
    (clean-queue-inside-loop)
    (Thread/sleep (* t 1000))))

;; Map[[type symbol date strike], Map[kw, dataval]]
;; Not used for now, let's just dump directly to file.
(def options-data (atom {}))
(defn add-options [old symb quote cur-time t opts]
  (reduce
   (fn [acc {:keys [strike expiration] :as opt}]
     (update
      acc [t symb expiration strike]
      (fn [old-data]
        (if old-data
          (conj old-data (merge opt quote {:req-time cur-time}))
          [(merge opt quote {:req-time cur-time})]))))
   old
   opts))

;; Agent to handle futures trying to write to different files
(def agent-options (agent nil))
(add-watch
 agent-options :append
 (fn [_key _ref _old {:keys [target data]}]
   (gz-write-line target data)))

;; Agent to handle futures trying to write to mariadb
(def agent-live-options (agent []))

;; Keep a list of all currently tracked options by the crawler,
;; reminder, this is not all options, only the ones seen since the crawler started.
(def options-set (atom #{}))
(defn add-tracked-options
  [old symb t opts]
  (reduce
   (fn [acc {:keys [strike expiration] :as opt}]
     (conj acc [t symb expiration strike]))
   old
   opts))

(defn append-options
  "Appends to `SYMBOL.txt.gz` AND to db all required data"
  [symb quote cur-time t opts]
  (let [{:keys [year month-of-year day-of-month]} (cur-ny-time)
        cur-date (str year (format "%02d" month-of-year) (format "%02d" day-of-month))
        path (str (:save-path config) "/" cur-date "/" symb ".txt.gz")
        data
        (map
         (fn [opt]
           (let [greeks (try
                          (gks/calculate-greeks
                           (assoc
                            opt
                            :opt-type t
                            :annual-dividend-rate (:trailingAnnualDividendRate quote)
                            :annual-dividend-yield (:tailingAnnualDividendYield quote)
                            :stock-price (:regularMarketPrice quote)))
                          (catch Exception e (do (println e) {})))]
             {:req-time cur-time
              :opt (merge
                    (assoc opt :opt-type t :quote-type (:quoteType quote))
                    greeks)
              :quote
              (dissoc
               quote
               ;; Remove redundant / useless data to save some space...
               :language :exchangeTimezoneName :region :currency :longName :displayName
               :shortName :market :exchange :messageBoardId)}))
         opts)]
    (io/make-parents path)
    (when (and quote (not-empty quote))
      (try
        (odb/insert-or-update-live-quote symb (json/write-str quote))
        (catch Exception e (error (str "SQL FAILURE quote for " symb)))))
    (when (not-empty data)
      (send agent-live-options (fn [old] (into old data)))
      (send agent-options (fn [old] {:target path :data (str/join "\n" data)})))))

(defn process-queue
  "Take a symbol, a date, a query entrypoint number, and process & adapt the queue
  Concretely, append all the data to the correct keys in `options` and add the missing
  expiration dates to the queue."
  [debug endpoint [symb date nb-views]]
  (when debug
    (info (str "Querying " symb " at date " (if date date "unset"))))
  (let [{:keys [instant-seconds offset-seconds]} (cur-ny-time)
        cur-time (+ instant-seconds offset-seconds)
        {:keys [expirationDates quote options status] :as data}
        (try
          (get-ticker-data endpoint symb date)
          (catch Exception e
            (do
              (when debug
                (error endpoint))
              (register-failure endpoint)
              nil)))
        {:keys [calls puts]} (first options)
        new-exp-dates (set/difference
                       (into #{} expirationDates)
                       (-> @symbols (get symb) :exp-dates))]
    ;; (swap! options-data add-options symb quote cur-time "C" calls)
    ;; (swap! options-data add-options symb quote cur-time "P" puts)
    (swap! options-set add-tracked-options symb "C" calls)
    (swap! options-set add-tracked-options symb "P" puts)
    (append-options symb quote cur-time "C" calls)
    (append-options symb quote cur-time "P" puts)
    (swap! symbols #(update-in % [symb :exp-dates] set/union new-exp-dates))
    (doseq [exp-date new-exp-dates]
      (swap! queue conj [symb exp-date 0]))
    (if (= status :success)
      (swap! queue conj [symb date (inc nb-views)])
      (swap! queue conj [symb date nb-views]))))

(defn scheduler
  "Basic scheduler that handles reorganisation of the queue & of the proxies,
  and saving the data"
  [config]
  ;; Re-order queue
  (info (str "Running scheduler (iteration: " @iter ")"))
  (swap! queue #(into [] (sort-by last %)))
  ;; Re-order proxies
  (sort-endpoints)
  (spit (str (:save-path config) "/status.edn") (pr-str @all-endpoints))
  (spit (str (:save-path config) "/queue.edn") (pr-str @queue))
  (info (str "Seen options: " (count @options-set) " | queue size: " (count @queue)))
  (let [data (take (:batch-size config) @agent-live-options)
        c-data (count data)]
    (try
      (info (str "SQL Write in progress, " c-data " rows."))
      (odb/insert-or-update-live data)
      (catch Exception e (error (str "SQL FAILURE " e))))
    (send agent-live-options (fn [old] (into [] (drop c-data old))))))

(defn crawler
  [init-queue]
  (if (nil? init-queue)
    (clean-queue-inside-loop)
    (reset! queue init-queue))
  ;; Trigger clean-queue-loop, if queue was empty, update is twice in a row, I can
  ;; live with that.
  (future (clean-queue-loop (:t-clean-queue config)))
  (let [nb-endpoints (:nb-endpoints config)
        old-time (atom 0)]
    (while (not-empty @queue)
      (let [cur-time (System/currentTimeMillis)
            is-market (market-time cur-time)
            cur-op (first @queue)
            endpoint (nth @available-endpoints (mod @iter nb-endpoints))]
        (if (or (not= "CLOSED" is-market) (:force-crawl config))
          (do
            (swap! queue #(->> % rest (into [])))
            (future (process-queue (:debug config) endpoint cur-op))
            (when (> (/ (- cur-time @old-time) 1000) (:t-reorder config))
              (reset! old-time cur-time)
              (scheduler config))
            (swap! iter inc))
          (do
            (when (= (mod @iter (* nb-endpoints 500)) 0)
              (info "Currently outside of [PRE|POST] market hours."))))
        (Thread/sleep (/ 2000 nb-endpoints))))
    (warn "Crawler coming to a halt.")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Functions that will be useful when we try to make the queue smart.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (defn append-to-queue
    "Takes a recently processed entry, and appends the next query time if it's
  not after expiration and not again outside of market hours (not need to request twice...)."
    [queue {:keys [ticker date next-time]}]
    (let [n-next-time (-> queue last :next-time inc get-time (max (+ 3600 next-time)))]
      (if (> n-next-time date)
        queue
        (conj queue {:ticker ticker :date date :next-time next-time})))))

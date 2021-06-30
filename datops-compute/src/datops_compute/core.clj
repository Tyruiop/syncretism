(ns datops-compute.core
  (:gen-class)
  (:require
   [taoensso.timbre :as timbre :refer [info warn error]]
   [datops-compute.db :as db]))

(defn -main
  [& args]
  (let [set-args (into #{} args)]
    (when (set-args "--init-db")
      (info "Initialize time series db")
      (db/timeseries-table-def))
    (when (set-args "--yields")
      (info "Updating yields across active options.")
      (db/update-live-options 10000)
      (info "Done."))))

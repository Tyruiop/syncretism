(ns datops-compute.core
  (:gen-class)
  (:require
   [taoensso.timbre :as timbre :refer [info warn error]]
   [datops-compute.db-update :as dbu]))

(defn -main
  [& args]
  (let [set-args (into #{} args)]
    (when (set-args "--yields")
      (info "Updating yields across active options.")
      (dbu/update-live-options 10000)
      (info "Done."))))

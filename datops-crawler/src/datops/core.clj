(ns datops.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [datops.db :as db]
   [datops.shared :as shared]
   [datops.options :as options]
   [datops.fundamentals :as fundamentals]))

(defn -main
  "Starts the option crawler, reloading the last saved queue if it exists."
  [& args]
  (if (= "--init" (first args))
    (db/init-db)
    (do
      (options/init-queue)
      (future (fundamentals/crawler))
      (future (options/crawler)))))

;; Start from here in emacs for live code reload
;; (-main)


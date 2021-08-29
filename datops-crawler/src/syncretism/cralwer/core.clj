(ns syncretism.crawler.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [syncretism.crawler.db :as db]
   [syncretism.crawler.shared :as shared]
   [syncretism.crawler.options :as options]
   [syncretism.crawler.fundamentals :as fundamentals]))

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


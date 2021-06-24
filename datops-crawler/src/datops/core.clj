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
    (let [fname (str (:save-path shared/config) "/queue.edn")]
      (future (fundamentals/crawler))
      (future
        (if (.exists (io/file fname))
          (options/crawler (-> fname slurp read-string distinct))
          (options/crawler nil))))))

;; Start from here in emacs for live code reload
;; (-main)

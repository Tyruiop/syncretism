(ns datops-compute.utils
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import
   [org.apache.commons.io Charsets]))

(defn read-gzipped
  [f fname]
  (with-open [in (java.util.zip.GZIPInputStream.
                  (io/input-stream fname))]
    (doall
     (map
      f
      (line-seq (java.io.BufferedReader.
                 (java.io.InputStreamReader. in Charsets/UTF_8)))))))

(defn find-tickers
  [dir]
  (let [;; Get last date's dir
        fdir (-> (io/file dir) file-seq last)]
    (keep
     (fn [f]
       (let [fname (str f)]
         (when (str/ends-with? fname ".txt.gz")
           (-> fname
               (str/split #"/")
               last
               (str/replace #".txt.gz" "")))))
     (file-seq fdir))))

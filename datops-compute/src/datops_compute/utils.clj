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
  "If lastday? is true, returns the tickers of the last day, if it's false
  then it is assumed that dir targets a specific day."
  ([dir] (find-tickers dir true))
  ([dir lastday?]
   (let [;; Get last date's dir
         fdir (if lastday?
                (->> (io/file dir) file-seq (filter #(.isDirectory %)) (sort-by str) last)
                (io/file dir))]
     (keep
      (fn [f]
        (let [fname (str f)]
          (when (str/ends-with? fname ".txt.gz")
            (-> fname
                (str/split #"/")
                last
                (str/replace #".txt.gz" "")))))
      (file-seq fdir)))))

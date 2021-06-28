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

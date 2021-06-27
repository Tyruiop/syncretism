(ns datops-compute.utils
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]))

(defn read-gzipped
  [fname]
  (with-open [in (java.util.zip.GZIPInputStream.
                  (io/input-stream fname))]
    (slurp in)))

(defn read-edn-per-line
  [in]
  (->> in
       str/split-lines
       (map read-string)))

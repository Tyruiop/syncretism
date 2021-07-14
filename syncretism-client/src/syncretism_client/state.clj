(ns syncretism-client.state
  (:require [clojure.java.io :as io]))

(def state (atom (-> "init.edn" io/resource slurp read-string)))

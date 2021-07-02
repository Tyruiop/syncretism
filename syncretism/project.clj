(defproject syncretism "0.1.0"
  :description "Common library for all syncretism projects"
  :url "https://github.com/Tyruiop/syncretism/tree/main/syncretism"
  :license {:name "GNU AGPL-V3 or later"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clojure.java-time "0.3.2"]
                 [generateme/fastmath "2.1.3"]]
  :repl-options {:init-ns syncretism.greeks})

(defproject datops "0.1.0"
  :description "Option data crawler"
  :url "https://github.com/Tyruiop/syncretism"
  :license {:name "GNU AGPL-V3 or later"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/data.csv "1.0.0"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [mysql/mysql-connector-java "8.0.23"]
                 [clojure.java-time "0.3.2"]
                 [clj-http "3.12.0"]
                 [com.taoensso/timbre "5.1.2"]
                 [com.velisco/clj-ftp "0.3.12"]]
  :main ^:skip-aot datops.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

(defproject datops-compute "0.1.0-SNAPSHOT"
  :description "Computations to be ran daily over option data"
  :url "https://github.com/Tyruiop/syncretism/tree/main/datops-compute"
  :license {:name "GNU AGPL-V3 or later"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [commons-io/commons-io "2.10.0"]
                 [com.climate/claypoole "1.1.4"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [com.taoensso/timbre "5.1.2"]
                 [mysql/mysql-connector-java "8.0.23"]

                 [org.clojars.tyruiop/syncretism "0.1.1"]]
  :main ^:skip-aot datops-compute.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})

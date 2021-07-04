(defproject datops-backend "0.1.0-SNAPSHOT"
  :description "Minimal backend to serve live option data"
  :license {:name "GNU AGPL-V3 or later"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :url "https://github.com/Tyruiop/syncretism"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [seancorfield/next.jdbc "1.1.613"]
                 [com.taoensso/timbre "5.1.2"]
                 [mysql/mysql-connector-java "8.0.23"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-cors "0.1.13"]
                 [org.clojars.tyruiop/syncretism "0.1.0"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler datops-backend.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})

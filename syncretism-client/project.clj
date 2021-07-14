(defproject syncretism-client "0.1.0-SNAPSHOT"
  :description "Syncretism standalone client"
  :url "https://github.com/Tyruiop/syncretism/tree/main/syncretism-client"
  :license {:name "GNU AGPL-V3 or later"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [clj-http "3.12.0"]
                 [io.github.spair/imgui-java-app "1.83.3"]]
  :main ^:skip-aot syncretism-client.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})

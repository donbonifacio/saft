(defproject saft "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha13"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.clojure/tools.cli "0.3.5"]
                 [mysql/mysql-connector-java "5.1.40"]
                 [clj-yaml "0.4.0"]]

  :main saft.core
  :uberjar-name "saft.jar"

  :aliases {"autotest" ["trampoline" "with-profile" "+test,+test-deps" "test-refresh"]
            "test"  ["trampoline" "with-profile" "+test,+test-deps" "test"]}

  :profiles {:uberjar {:aot :all}
             :test-deps {:dependencies [[org.clojure/tools.namespace "0.2.11"]]

                         :plugins [[com.jakemccrary/lein-test-refresh "0.15.0"]
                                   [venantius/ultra "0.5.0"]]}}

  :test-refresh {:quiet true
                 :changes-only true}
  )


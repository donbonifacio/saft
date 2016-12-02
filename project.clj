(defproject saft "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha13"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [mysql/mysql-connector-java "6.0.5"]]

  :aliases {"autotest" ["trampoline" "with-profile" "+test,+test-deps" "test-refresh"]
            "test"  ["trampoline" "with-profile" "+test,+test-deps" "test"]}

  :profiles {:test-deps {:dependencies [[org.clojure/tools.namespace "0.2.11"]]

                         :plugins [[com.jakemccrary/lein-test-refresh "0.15.0"]
                                   [venantius/ultra "0.5.0"]]}}

  :test-refresh {:quiet true
                 :changes-only true}
  )


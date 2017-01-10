(ns saft.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.core :as core]))

(deftest local-db-test
  #_(core/generate-saft {:account-id 5554 #_6599
                       :formatted true
                       ;:begin "2012-01-01" :end "2012-12-31"
                       :begin "2016-01-01" :end "2016-12-31"
                       :preload-all-documents? true
                       :output "tmp/saft.xml"}))

(deftest clients-by-version-test
  (let [client-versions [{:client_id 1 :version 1}
                         {:client_id 1 :version 2}
                         {:client_id 2 :version 1}]]
    (is (= (core/clients-by-version client-versions)
           {[1 1] [(first client-versions)]
            [1 2] [(second client-versions)]
            [2 1] [(nth client-versions 2)]}))))

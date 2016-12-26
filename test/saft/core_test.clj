(ns saft.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.core :as core]))

(deftest a-test
  (core/generate-saft {:account-id 5554 #_6599
                       :formatted true
                       ;:begin "2012-01-01" :end "2012-12-31"
                       :begin "2016-12-01" :end "2016-12-31"
                       :preload-all-documents? true
                       :output "tmp/saft.xml"}))

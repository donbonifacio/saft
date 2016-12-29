(ns saft.accounting-relevant-totals-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.accounting-relevant-totals :as accounting-relevant-totals]))

(deftest totals-xml-test
  (is (= (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
              "<NumberOfEntries>10</NumberOfEntries>"
              "<TotalDebit>10.4</TotalDebit>"
              "<TotalCredit>5.88</TotalCredit>")
         (-> {:number_of_entries 10
              :total_debit 10.4
              :total_credit 5.88}
             (accounting-relevant-totals/totals-xml)
             (xml/emit-str)))))

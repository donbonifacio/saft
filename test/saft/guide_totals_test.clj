(ns saft.guide-totals-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.guide-totals :as guide-totals]))

(deftest guide-totals-xml-test
  (is (= (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
              "<NumberOfMovementLines>10</NumberOfMovementLines>"
              "<TotalQuantityIssued>10.4</TotalQuantityIssued>")
         (-> {:line_count 10
              :quantity 10.4}
             (guide-totals/totals-xml)
             (xml/emit-str)))))

(ns saft.client-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.client :as client]))

(deftest client-xml-test
  (let [client
          {:id 206417
           :fiscal_id "123123123"
           :name "Claudinha"}

        expected
          (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                "<Customer>"
                  "<CustomerID>206417</CustomerID>"
                  "<AccountID>Desconhecido</AccountID>"
                  "<CustomerTaxID>123123123</CustomerTaxID>"
                  "<CompanyName>Claudinha</CompanyName>"
                  "<BillingAddress>"
                    "<AddressDetail>Desconhecido</AddressDetail>"
                    "<City>Desconhecido</City>"
                    "<PostalCode>0000-000</PostalCode>"
                    "<Country>PT</Country>"
                  "</BillingAddress>"
                  "<Telephone>Desconhecido</Telephone>"
                  "<Fax>Desconhecido</Fax>"
                  "<Email>Desconhecido</Email>"
                  "<Website>Desconhecido</Website>"
                  "<SelfBillingIndicator>0</SelfBillingIndicator>"
                "</Customer>")]
    (is (= expected
           (xml/emit-str (client/client-xml client))))))


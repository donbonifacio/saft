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

(deftest final-consumer?-test
  (is (true? (client/final-consumer? {:id 10 :fiscal_id nil})))
  (is (true? (client/final-consumer? {:id 10 :fiscal_id ""})))
  (is (false? (client/final-consumer? {:id 10 :fiscal_id "1234"}))))

(deftest fiscal-id-test
  (is (= client/final-consumer-fiscal-id (client/fiscal-id {})))
  (is (= client/final-consumer-fiscal-id (client/fiscal-id {:fiscal_id nil})))
  (is (= client/final-consumer-fiscal-id (client/fiscal-id {:fiscal_id ""})))
  (is (= "1234" (client/fiscal-id {:fiscal_id "1234"}))))

(deftest portuguese?-test
  (is (true? (client/portuguese? {:country "Portugal"})))
  (is (true? (client/portuguese? {:country nil})))
  (is (false? (client/portuguese? {:country "Alaska"}))))

(deftest postal-code-test
  (is (= "1234-123" (client/postal-code {:country "Portugal" :postal_code "1234-123"})))
  (is (= "1234-123" (client/postal-code {:country "Portugal" :postal_code "1234-123 Amadora"})))
  (is (= "1234-123" (client/postal-code {:country nil :postal_code "1234-123 Amadora"})))
  (is (= "0000-000" (client/postal-code {:country "Portugal" :postal_code nil})))
  (is (= "0000-000" (client/postal-code {:country "Germany" :postal_code nil})))
  (is (= "1234 Fora de Portug" (client/postal-code {:country "Germany" :postal_code "1234 Fora de Portugal"})))
  )

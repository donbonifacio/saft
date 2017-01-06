(ns saft.document-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.document :as document]))

(deftest invoice-xml-test
  (let [account
          {:id 5554}

        invoice
          {:id 206417
           :date "2016-01-01"
           :created_at "2016-07-01T15:06:33"
           :final_date "2016-07-01T15:06:33"
           :saft_hash "N9rCtcNw7IPkCZQa7rOS28nxcb1AerYOJI8cJZaGuxPwgzWHCzIAsF8B2C5VK5tso6Bqe+pu0ixTgYgehxAwLeK9s9tT4IJMDBlodAwi9lzCdvq2GKU3NwT7aId+3ODyKBYoERAu+wxWAN7Qq+W9cOC7K4FeTbYLgWN2PqP9NIs="
           :sequence_number "FT 2013/69"
           :document_serie "2013"
           :document_number "69"
           :status "sent"
           :tax 3.68
           :total 19.68M
           :total_taxes 3.68M
           :total_with_taxes 19.68M
           :total_before_taxes 16.00M
           :items [{:name "Item 8"
                    :description "D5c194mpfz8h"
                    :quantity "2.0"
                    :unit_price "8.0"
                    :unit "unit"
                    :subtotal "16.0"
                    :tax_value 23.0
                    :tax_point_date "2016-01-01"}]}

        expected
          (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                "<Invoice>"
                  "<InvoiceNo>FT 2013/69</InvoiceNo>"
                  "<DocumentStatus>"
                    "<InvoiceStatus>N</InvoiceStatus>"
                    "<InvoiceStatusDate>2016-07-01T15:06:33</InvoiceStatusDate>"
                    "<SourceID>5554</SourceID>"
                    "<SourceBilling>P</SourceBilling>"
                  "</DocumentStatus>"
                  "<Hash>N9rCtcNw7IPkCZQa7rOS28nxcb1AerYOJI8cJZaGuxPwgzWHCzIAsF8B2C5VK5tso6Bqe+pu0ixTgYgehxAwLeK9s9tT4IJMDBlodAwi9lzCdvq2GKU3NwT7aId+3ODyKBYoERAu+wxWAN7Qq+W9cOC7K4FeTbYLgWN2PqP9NIs=</Hash>"
                  "<HashControl>1</HashControl>"
                  "<Period>1</Period>"
                  "<InvoiceDate>2016-01-01</InvoiceDate>"
                  "<InvoiceType>FT</InvoiceType>"
                  "<SpecialRegimes>"
                    "<SelfBillingIndicator>0</SelfBillingIndicator>"
                    "<CashVATSchemeIndicator>0</CashVATSchemeIndicator>"
                    "<ThirdPartiesBillingIndicator>0</ThirdPartiesBillingIndicator>"
                  "</SpecialRegimes>"
                  "<SourceID>5554</SourceID>"
                  "<SystemEntryDate>2016-07-01T15:06:33</SystemEntryDate>"
                  "<CustomerID>0</CustomerID>"
                  "<Line>"
                    "<LineNumber>1</LineNumber>"
                    "<ProductCode>Item 8</ProductCode>"
                    "<ProductDescription>D5c194mpfz8h</ProductDescription>"
                    "<Quantity>2.0</Quantity>"
                    "<UnitOfMeasure>unit</UnitOfMeasure>"
                    "<UnitPrice>8.0</UnitPrice>"
                    "<TaxPointDate>2016-01-01</TaxPointDate>"
                    "<Description>D5c194mpfz8h</Description>"
                    "<CreditAmount>16.0</CreditAmount>"
                    "<Tax>"
                      "<TaxType>IVA</TaxType>"
                      "<TaxCountryRegion>PT</TaxCountryRegion>"
                      "<TaxCode>NOR</TaxCode>"
                      "<TaxPercentage>23.0</TaxPercentage>"
                    "</Tax>"
                  "</Line>"
                  "<DocumentTotals>"
                    "<TaxPayable>3.68</TaxPayable>"
                    "<NetTotal>16.00</NetTotal>"
                    "<GrossTotal>19.68</GrossTotal>"
                  "</DocumentTotals>"
                "</Invoice>")]
    (is (= expected
           (xml/emit-str (document/document-xml {} account invoice))))))

(deftest prepare-items-test
  (let [document {:id 1}
        account {}
        items [{:id 1}]
        cache {:items {1 items}}]
    (is (= items
           (:items (document/prepare-items cache account document))))
    (is (= items
          (:items (document/prepare-items {} account (assoc document :items items)))))))

(deftest convert-factura-recibo-test
  (is (= "CreditNote" (document/convert-factura-recibo {} "CreditNote")))
  (is (= "Invoice" (document/convert-factura-recibo {} "Invoice")))
  (is (= "InvoiceReceipt" (document/convert-factura-recibo {} "InvoiceReceipt")))
  (is (= nil (document/convert-factura-recibo {} nil)))

  (is (= "InvoiceReceipt" (document/convert-factura-recibo
                            {:factura_recibo true} "Invoice"))))

(deftest type-code-test
  (is (= "FT" (document/type-code {} nil)))
  (is (= "FT" (document/type-code {} "Invoice")))
  (is (= "FR" (document/type-code {} "InvoiceReceipt")))
  (is (= "FS" (document/type-code {} "SimplifiedInvoice")))
  (is (= "ND" (document/type-code {} "DebitNote")))
  (is (= "NC" (document/type-code {} "CreditNote")))
  (is (= "VD" (document/type-code {} "CashInvoice")))
  (is (= "FR" (document/type-code {:factura_recibo true} "Invoice"))))

(deftest saft-status-test
  (is (= "A" (document/invoice-status {:status "canceled"})))
  (is (= "N" (document/invoice-status {:status "sent"})))
  (is (= "N" (document/invoice-status {:status "settled"}))))

(deftest number-test
  (is (= "FT A/1" (document/number {} {} {:type "Invoice"
                                        :document_serie "A"
                                        :document_number "1"}))))

(deftest guide-number-test
  (is (= "GR A/1" (document/guide-number {} {} {:type "Shipping"
                                                :document_serie "A"
                                                :document_number "1"}))))

(deftest total-taxes-test
  (is (= 2 (document/total-taxes {:total 10 :total_before_taxes 8})))
  (is (= 2 (document/total-taxes {:total 10 :total_before_taxes 8 :retention 0})))
  (is (= 2 (document/total-taxes {:retention 10 :total_taxes 2}))))

(deftest gross-total-test
  (is (= 12 (document/gross-total {:total_before_taxes 10 :total_taxes 2}))))

(deftest retention-test
  (is (= 100 (document/retention {:retention 100 :total_before_taxes 100}))))

(deftest client-test
  (let [client {:id 1 :version 1}
        cache {:clients {[1 1] [client]}}]
    (is (= client (document/client cache {:client_id 1 :client_version 1})))))

(deftest customer-id-test
  (let [client {:id 1 :version 1 :fiscal_id "123"}
        cache {:clients {[1 1] [client]}}]
    (is (= 0 (document/customer-id cache {})))
    (is (= 1 (document/customer-id cache {:client_id 1 :client_version 1})))))

(deftest owner-invoice-number-test
  (is (nil? (document/owner-invoice-number {} {} {})))
  (is (nil? (document/owner-invoice-number {} {} {:id 1 :owner_invoice_id 1})))

  (testing "Usual scenario, document references other document"
    (let [document {:owner_invoice_id 1}
          owner-document {:id 1 :document_number 1 :document_serie "A"}
          account {}
          cache {:owner-documents {1 [owner-document]}}]
      (is (= "FT A/1" (document/owner-invoice-number cache account document)))))

  (testing "Usual scenario, document references other guide"
    (let [document {:owner_invoice_id 1}
          owner-document {:id 1 :document_number 1 :document_serie "A" :type "Shipping"}
          account {}
          cache {:owner-documents {1 [owner-document]}}]
      (is (= "GR A/1" (document/owner-invoice-number cache account document)))))

  (testing "Document has raw_owner_invoice"
    (let [document {:owner_invoice_id 1 :raw_owner_invoice "FT A/2"}
          account {}
          cache {}]
      (is (= "FT A/2" (document/owner-invoice-number cache account document))))))

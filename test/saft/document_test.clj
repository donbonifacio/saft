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
           :tax "3.68"
           :total "16.00"
           :total_with_taxes "19.68"
           :items [{:name "Item 8"
                    :description "D5c194mpfz8h"
                    :quantity "2.0"
                    :unit_price "8.0"
                    :unit "unit"
                    :credit "16.0"
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

(deftest type-code-test
  (is (= "FT" (document/type-code {} nil)))
  (is (= "FT" (document/type-code {} "Invoice")))
  (is (= "FR" (document/type-code {} "InvoiceReceipt")))
  (is (= "FS" (document/type-code {} "SimplifiedInvoice")))
  (is (= "ND" (document/type-code {} "DebitNote")))
  (is (= "NC" (document/type-code {} "CreditNote")))
  (is (= "VD" (document/type-code {} "CashInvoice")))
  (is (= "FR" (document/type-code {:factura_recibo true} "Invoice"))))

(deftest number-test
  (= "FT A/1" (document/number {} {} {:type "Invoice"
                                      :document_serie "A"
                                      :document_number "1"})))

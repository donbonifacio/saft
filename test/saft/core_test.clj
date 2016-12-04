(ns saft.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.core :as core]))

(deftest a-test
  (core/generate-saft {:account-id 5554 #_6599
                       :formatted true
                       :begin "2012-01-01"
                       :end "2012-12-31"
                       :output "tmp/saft.xml"}))

(deftest header-xml-test
  (let [args
          {:year 2016
           :start-date "2016-01-01"
           :end-date "2016-12-31"
           :created "2016-12-02"}

        account
          {:fiscal_id "999999990"
           :organization_name "Pedro Santos"
           :postal_code "2855-097"
           :address "RUA RODRIGUES FARIA, 309, LX FACTORYPISO 4, SALA 10B"
           :city "LISBON - PORTUGAL"
           :email "donbonifacio@gmail.com"}

        expected
          (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                "<Header>"
                  "<AuditFileVersion>1.03_01</AuditFileVersion>"
                  "<CompanyID>999999990</CompanyID>"
                  "<TaxRegistrationNumber>999999990</TaxRegistrationNumber>"
                  "<TaxAccountingBasis>F</TaxAccountingBasis>"
                  "<CompanyName>Pedro Santos</CompanyName>"
                  "<CompanyAddress>"
                    "<AddressDetail>RUA RODRIGUES FARIA, 309, LX FACTORYPISO 4, SALA 10B</AddressDetail>"
                    "<City>LISBON - PORTUGAL</City>"
                    "<PostalCode>2855-097</PostalCode>"
                    "<Country>PT</Country>"
                  "</CompanyAddress>"
                  "<FiscalYear>2016</FiscalYear>"
                  "<StartDate>2016-01-01</StartDate>"
                  "<EndDate>2016-12-31</EndDate>"
                  "<CurrencyCode>EUR</CurrencyCode>"
                  "<DateCreated>2016-12-02</DateCreated>"
                  "<TaxEntity>Global</TaxEntity>"
                  "<ProductCompanyTaxID>508025338</ProductCompanyTaxID>"
                  "<SoftwareCertificateNumber></SoftwareCertificateNumber>"
                  "<ProductID></ProductID>"
                  "<ProductVersion>1.0</ProductVersion>"
                "</Header>")]
    (is (= expected
           (xml/emit-str (core/header-xml args account))))))

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
           (xml/emit-str (core/client-xml client))))))

(deftest product-xml-test
  (let [product
          {:id 206417
           :name "Item 3"
           :description "w2WxmKul00"}

        expected
          (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                "<Product>"
                  "<ProductType>S</ProductType>"
                  "<ProductCode>Item 3</ProductCode>"
                  "<ProductDescription>w2WxmKul00</ProductDescription>"
                  "<ProductNumberCode>Item 3</ProductNumberCode>"
                "</Product>")]
    (is (= expected
           (xml/emit-str (core/product-xml product))))))


(deftest invoice-xml-test
  (let [account
          {:id 5554}

        invoice
          {:id 206417
           :date "2016-01-01"
           :created_at "2016-07-01T15:06:33"
           :saft_hash "N9rCtcNw7IPkCZQa7rOS28nxcb1AerYOJI8cJZaGuxPwgzWHCzIAsF8B2C5VK5tso6Bqe+pu0ixTgYgehxAwLeK9s9tT4IJMDBlodAwi9lzCdvq2GKU3NwT7aId+3ODyKBYoERAu+wxWAN7Qq+W9cOC7K4FeTbYLgWN2PqP9NIs="
           :sequence_number "FT 2013/69"
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
                    "<InvoiceStatus>A</InvoiceStatus>"
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
           (xml/emit-str (core/invoice-xml {} account invoice))))))


(ns saft.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.core :as core]))

(deftest a-test
  (core/foo))

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

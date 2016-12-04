(ns saft.header-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.header :as header]))

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
           (xml/emit-str (header/header-xml args account))))))

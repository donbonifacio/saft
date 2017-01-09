(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.header
  "SAF-T Header handling."
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn header-xml
  "Generates the XML elements of a SAF-T header."
  [args account]
  (xml/element :Header {}
               (xml/element :AuditFileVersion {} "1.03_01")
               (xml/element :CompanyID {} (:fiscal_id account))
               (xml/element :TaxRegistrationNumber {} (:fiscal_id account))
               (xml/element :TaxAccountingBasis {} "F")
               (xml/element :CompanyName {} (:organization_name account))
               (xml/element :CompanyAddress {}
                            (xml/element :AddressDetail {} (:address account))
                            (xml/element :City {} (:city account))
                            (xml/element :PostalCode {} (:postal_code account))
                            (xml/element :Country {} "PT"))
               (xml/element :FiscalYear {} (:year args))
               (xml/element :StartDate {} (:start-date args))
               (xml/element :EndDate {} (:end-date args))
               (xml/element :CurrencyCode {} "EUR")
               (xml/element :DateCreated {} (:created args))
               (xml/element :TaxEntity {} "Global")
               (xml/element :ProductCompanyTaxID {} "508025338")
               (xml/element :SoftwareCertificateNumber {})
               (xml/element :ProductID {})
               (xml/element :ProductVersion {} "1.0")))

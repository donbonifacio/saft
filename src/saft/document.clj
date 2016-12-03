(ns saft.document
  (:require
    [clojure.data.xml :as xml]
    [saft.common :as common]))

(defn item-xml [idx item]
  (xml/element :Line {}
               (xml/element :LineNumber {} (inc idx))
               (xml/element :ProductCode {} (common/get-str item :name))
               (xml/element :ProductDescription {} (common/get-str item :description))
               (xml/element :Quantity {} (:quantity item))
               (xml/element :UnitOfMeasure {} (:unit item))
               (xml/element :UnitPrice {} (:unit_price item))
               (xml/element :TaxPointDate {} (:tax_point_date item))
               (xml/element :Description {} (common/get-str item :description))
               (xml/element :CreditAmount {} (:credit item))
               (xml/element :Tax {}
                            (xml/element :TaxType {} "IVA")
                            (xml/element :TaxCountryRegion {} "PT")
                            (xml/element :TaxCode {} "NOR")
                            (xml/element :TaxPercentage {} "23.0"))))

(defn document-xml
  [account doc]
  (xml/element :Invoice {}
                 (xml/element :InvoiceNo {} (str (:sequence_number doc)))
                 (xml/element :DocumentStatus {}
                              (xml/element :InvoiceStatus {} "A")
                              (xml/element :InvoiceStatusDate {} "2016-07-01T15:06:33")
                              (xml/element :SourceID {} (:id account))
                              (xml/element :SourceBilling {} "P"))
                 (xml/element :Hash {} (:saft_hash doc))
                 (xml/element :HashControl {} 1)
                 (xml/element :Period {} 1)
                 (xml/element :InvoiceDate {} (common/get-date doc :date))
                 (xml/element :InvoiceType {} "FT")
                 (xml/element :SpecialRegimes {}
                              (xml/element :SelfBillingIndicator {} 0)
                              (xml/element :CashVATSchemeIndicator {} 0)
                              (xml/element :ThirdPartiesBillingIndicator {} 0))
                 (xml/element :SourceID {} (:id account))
                 (xml/element :SystemEntryDate {} (common/get-date doc :created_at))
                 (xml/element :CustomerID {} 0)
                 (map-indexed item-xml (:items doc))
                 (xml/element :DocumentTotals {}
                              (xml/element :TaxPayable {} (:tax doc))
                              (xml/element :NetTotal {} (:total doc))
                              (xml/element :GrossTotal {} (:total_with_taxes doc)))))

(ns saft.guide
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.account :as account]
    [saft.document :as document]
    [saft.payment-method :as payment-method]
    [saft.guide-item :as guide-item]
    [saft.item :as item]))

(defn guides-query
  [{:keys [db account-id account begin end]}]
  (common/query-time-info "[SQL] Fetch guides"
     (j/query db [(str " select invoices.id, type, sequence_number,
                            document_number, document_serie,
                            retention, loaded_at, at_doc_code_id,
                            total, total_taxes, total_before_taxes,
                            invoices.account_id, account_version, saft_hash,
                            invoices.created_at, invoices.updated_at, final_date, date,
                            client_id, client_version, owner_invoice_id,
                            tax_exemption_message,
                            address_from.detail as address_from_detail,
                            address_from.city as address_from_city,
                            address_from.postal_code as address_from_postal_code,
                            tax_location_from.name as address_from_country,
                            address_to.detail as address_to_detail,
                            address_to.city as address_to_city,
                            address_to.postal_code as address_to_postal_code,
                            tax_location_to.name as address_to_country
                         from invoices
                         inner join clients on (invoices.client_id = clients.id)
                         inner join addresses as address_to on (invoices.address_to_id = address_to.id)
                         inner join addresses as address_from on (invoices.address_from_id = address_from.id)
                         inner join tax_countries as tax_location_to on (tax_location_to.id = address_to.tax_location_id)
                         inner join tax_countries as tax_location_from on (tax_location_from.id = address_from.tax_location_id)
                         where " (common/saft-guides-condition account) "
                               and (invoices.status in ("(common/saft-status-str)"))
                               and invoices.account_id = " (:id account) "
                               and (invoices.date between '" begin "' and '" end "')
                               and invoices.account_reset_id is null
                               and tax_location_from.name = 'Portugal'
                               and tax_location_to.name = 'Portugal'
                               and clients.country = 'Portugal'")])))

(defn address [doc k]
  (xml/element :Address {}
               (xml/element :AddressDetail {} (common/get-str doc (keyword (str "address_" k "_detail")) 10))
               (xml/element :City {} (common/get-str doc (keyword (str "address_" k "_city")) 50))
               (xml/element :PostalCode {} (get doc (keyword (str "address_" k "_postal_code"))))
               (xml/element :Country {}
                            (if (= "Portugal" (get doc (keyword (str "address_" k "_country"))))
                               "PT"
                               common/unknown))))

(defn movement-start-time [doc]
  (common/saft-date (or (:loaded_at doc)
                        (:final_date doc)
                        (:updated_at doc))))

(defn guide-xml
  [cache account doc]
  (let [account-version (account/for-document cache account doc)
        items (get-in cache [:items (:id doc)])]
    (xml/element :StockMovement {}
                 (xml/element :DocumentNumber {} (document/guide-number cache account doc))
                 (xml/element :DocumentStatus {}
                              (xml/element :MovementStatus {} (document/invoice-status doc))
                              (xml/element :MovementStatusDate {} (document/final-date doc))
                              (xml/element :SourceID {} (:id account))
                              (xml/element :SourceBilling {} "P"))
                 (xml/element :Hash {} (:saft_hash doc))
                 (xml/element :MovementDate {} (when (:final_date doc) (common/get-date doc :final_date)))
                 (xml/element :MovementType {} (document/movement-type (:type doc)))
                 (xml/element :SystemEntryDate {} (document/final-date doc))
                 (xml/element :CustomerID {} (document/customer-id cache doc))
                 (xml/element :SourceID {} (:id account))
                 (xml/element :ShipTo {} (address doc "to"))
                 (xml/element :ShipFrom {} (address doc "from"))
                 (xml/element :MovementStartTime {} (movement-start-time doc))
                 (when (:at_doc_code_id doc)
                   (xml/element :AtDocCodeID {} (:at_doc_code_id doc)))
                 (map-indexed #(guide-item/guide-item-xml %1
                                                          (document/client cache doc)
                                                          doc
                                                          %2
                                                          cache)
                              items)
                 (xml/element :DocumentTotals {}
                               (xml/element :TaxPayable {} (document/total-taxes doc))
                               (xml/element :NetTotal {} (:total_before_taxes doc))
                               (xml/element :GrossTotal {} (document/gross-total doc)))
                 
                 )))

(ns saft.document
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.item :as item]))

(defn documents-query
  [{:keys [db account-id account begin end]}]
  (common/time-info "[SQL] Fetch documents"
     (j/query db [(str "select id, sequence_number,
                          account_id, account_version
                        from invoices
                        where account_id = ?
                          and " (common/saft-types-condition account) "
                          and status in (" (common/saft-status-str)  ")
                          and (invoices.date between '" begin "' and '" end "')
                        order by invoices.id asc;")
                  account-id])))

(defn prepare-items [cache account doc]
  (cond
    (some? (:items doc)) doc
    (some? (get-in cache [:items (:id doc)])) (assoc doc :items (get-in cache [:items (:id doc)]))
    :else (assert nil "No items!")))

(defn document-xml
  [cache account doc]
  (let [doc (prepare-items cache account doc)]
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
                   (map-indexed item/item-xml (:items doc))
                   (xml/element :DocumentTotals {}
                                (xml/element :TaxPayable {} (:tax doc))
                                (xml/element :NetTotal {} (:total doc))
                                (xml/element :GrossTotal {} (:total_with_taxes doc))))))

(ns saft.document
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.account :as account]
    [saft.item :as item]))

(defn documents-query
  [{:keys [db account-id account begin end]}]
  (common/time-info "[SQL] Fetch documents"
     (j/query db [(str "select id, type, sequence_number,
                          document_number, document_serie,
                          account_id, account_version, saft_hash,
                          created_at, updated_at, final_date, date
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

(def type-hash
  {nil "FT"
   "Invoice" "FT"
   "FacturaRecibo" "FR"
   "InvoiceReceipt" "FR"
   "SimplifiedInvoice" "FS"
   "DebitNote" "ND"
   "CreditNote" "NC"
   "CashInvoice" "VD"})

(defn convert-factura-recibo
  "Converts Invoice type in invoice Receit based on account"
  [account type-name]
  (if (and (or (= "Invoice" type-name)
               (nil? type-name))
           (:factura_recibo account))
    "InvoiceReceipt"
    type-name))

(defn type-code
  "Transforms a type name in a type code. Considers account version"
  [account type-name]
  (let [type-name (convert-factura-recibo account type-name)
        code (get type-hash type-name)]
    (assert code (str "No code for " type-name))
    code))

(defn invoice-status [doc]
  (if (= "canceled" (:status doc))
    "A"
    "N"))

(defn number
  "Gets the proper document number. Tries to get the account version
  for the document via the cache."
  [cache account doc]
  (str (type-code (account/for-document cache account doc) (:type doc))
       " "
       (:document_serie doc)
       "/"
       (:document_number doc)))

(defn final-date [doc]
  (common/saft-date (or (:final_date doc)
                        (:updated_at doc))))

(defn document-xml
  [cache account doc]
  (let [doc (prepare-items cache account doc)]
    (xml/element :Invoice {}
                   (xml/element :InvoiceNo {} (number cache account doc))
                   (xml/element :DocumentStatus {}
                                (xml/element :InvoiceStatus {} (invoice-status doc))
                                (xml/element :InvoiceStatusDate {} (final-date doc))
                                (xml/element :SourceID {} (:id account))
                                (xml/element :SourceBilling {} "P"))
                   (xml/element :Hash {} (:saft_hash doc))
                   (xml/element :HashControl {} 1)
                   (xml/element :Period {} (common/month (:date doc)))
                   (xml/element :InvoiceDate {} (common/get-date doc :date))
                   (xml/element :InvoiceType {} (type-code (account/for-document cache account doc) (:type doc)))
                   (xml/element :SpecialRegimes {}
                                (xml/element :SelfBillingIndicator {} 0)
                                (xml/element :CashVATSchemeIndicator {} 0)
                                (xml/element :ThirdPartiesBillingIndicator {} 0))
                   (xml/element :SourceID {} (:id account))
                   (xml/element :SystemEntryDate {} (final-date doc))
                   (xml/element :CustomerID {} 0)
                   (map-indexed item/item-xml (:items doc))
                   (xml/element :DocumentTotals {}
                                (xml/element :TaxPayable {} (:tax doc))
                                (xml/element :NetTotal {} (:total doc))
                                (xml/element :GrossTotal {} (:total_with_taxes doc))))))

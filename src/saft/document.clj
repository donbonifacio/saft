(ns saft.document
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.account :as account]
    [saft.item :as item]))

(defn documents-query
  [{:keys [db account-id account begin end]}]
  (common/query-time-info "[SQL] Fetch documents"
     (j/query db [(str "select id, type, sequence_number,
                          document_number, document_serie,
                          retention,
                          total, total_taxes, total_before_taxes,
                          account_id, account_version, saft_hash,
                          created_at, updated_at, final_date, date,
                          client_id, client_version, owner_invoice_id,
                          tax_exemption_message
                        from invoices
                        where account_id = ?
                          and " (common/saft-types-condition account) "
                          and status in (" (common/saft-status-str)  ")
                          and (invoices.date between '" begin "' and '" end "')
                        order by invoices.id asc;")
                  account-id])))

(defn owner-documents-query
  [{:keys [db account-id account begin end]} documents]
  (let [doc-ids (doall (distinct (keep :owner_invoice_id documents)))]
    (common/query-time-info (str "[SQL] Fetch " (count doc-ids)  " owner documents")
      (if (empty? doc-ids)
        []
        (j/query db [(str "select id, document_number, document_serie,
                                  raw_owner_invoice, type, total_taxes
                          from invoices
                          where (id <> owner_invoice_id or owner_invoice_id is null)
                                and id in (" (clojure.string/join "," doc-ids) ")")])))))

(defn documents-by-ids-query
  [{:keys [db account-id account begin end]} doc-ids]
  (common/query-time-info (str "[SQL] Fetch " (count doc-ids)  " documents by ids")
    (if (empty? doc-ids)
      []
      (j/query db [(str "select id, document_number, document_serie,
                                raw_owner_invoice, type, total_taxes, date
                        from invoices
                        where id in (" (clojure.string/join "," doc-ids) ")")]))))

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

(defn movement-type [type-name]
  (case type-name
    "Shipping" "GR"
    "Transport" "GT"
    "Devolution" "GD"))

(defn guide-number
  "Gets the proper guide document number. Tries to get the account version
  for the document via the cache."
  [cache account doc]
  (str (movement-type (:type doc))
       " "
       (:document_serie doc)
       "/"
       (:document_number doc)))

(defn final-date [doc]
  (common/saft-date (or (:final_date doc)
                        (:updated_at doc))))

(defn total-taxes [doc]
  (let [retention (:retention doc)]
    (if (and (some? retention) (pos? retention))
      (:total_taxes doc)
      (- (:total doc) (:total_before_taxes doc)))))

(defn gross-total [doc]
  (+ (:total_before_taxes doc) (:total_taxes doc)))

(defn retention [doc]
  (*
   (/ (:retention doc) 100)
   (:total_before_taxes doc)))

(defn client [cache doc]
  (first (get-in cache [:clients [(:client_id doc) (:client_version doc)]])))

(defn customer-id [cache doc]
  (if-let [client (client cache doc)]
    (if (or (nil? (:fiscal_id client)) (empty? (:fiscal_id client)))
      0
      (:id client))
    0))

(defn owner-invoice-number [cache account doc]
  (let [owner-invoice-id (:owner_invoice_id doc)]
    (if (or (nil? owner-invoice-id)
            (= owner-invoice-id (:id doc)))
      nil
      (let [owner-invoice (first (get-in cache [:owner-documents owner-invoice-id]))]
        (assert owner-invoice (str "No document for owner-invoice-id " owner-invoice-id " - invoice " (:id doc)))
        (cond
          (some? (:raw_owner_invoice owner-invoice))
            (:raw_owner_invoice owner-invoice)
          (common/guide? (:type owner-invoice))
            (guide-number owner-invoice)
          :else
            (number cache account owner-invoice))))))

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
                   (xml/element :CustomerID {} (customer-id cache doc))
                   (map-indexed (fn item-xml [idx item]
                                  (item/item-xml idx
                                                 (client cache doc)
                                                 doc
                                                 (owner-invoice-number cache account doc)
                                                 item))
                                (:items doc))
                   (xml/element :DocumentTotals {}
                                (xml/element :TaxPayable {} (total-taxes doc))
                                (xml/element :NetTotal {} (:total_before_taxes doc))
                                (xml/element :GrossTotal {} (gross-total doc)))
                   (when (and (some? (:retention doc)) (pos? (:retention doc)))
                     (xml/element :WithholdingTax {}
                                  (xml/element :WithholdingTaxAmount {} (retention doc)))))))

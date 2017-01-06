(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.document
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.account :as account]
    [saft.item :as item]))

(defn documents-query
  "Returns all the invoicing documents for the given period."
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
  "Returns all the owner documents for the given documents."
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
  "Returns all documents for the given document ids."
  [{:keys [db account-id account begin end]} doc-ids]
  (common/query-time-info (str "[SQL] Fetch " (count doc-ids)  " documents by ids")
    (if (empty? doc-ids)
      []
      (j/query db [(str "select id, document_number, document_serie,
                                raw_owner_invoice, type, total_taxes, date
                        from invoices
                        where id in (" (clojure.string/join "," doc-ids) ")")]))))

(defn prepare-items
  "Adds the items to the doc."
  [cache account doc]
  (cond
    (some? (:items doc)) doc
    (some? (get-in cache [:items (:id doc)])) (assoc doc :items (get-in cache [:items (:id doc)]))
    :else (assert nil "No items!")))

(def type-hash
  "Maps a type name to a type code."
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

(defn invoice-status
  "The saft status of a document."
  [doc]
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

(defn movement-type
  "Converts a guide type to a guide code."
  [type-name]
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

(defn final-date
  "The final date of a document."
  [doc]
  (common/saft-date (or (:final_date doc)
                        (:updated_at doc))))

(defn total-taxes
  "Gets the total taxes related to the given document."
  [doc]
  (let [retention (:retention doc)]
    (if (and (some? retention) (pos? retention))
      (:total_taxes doc)
      (- (:total doc) (:total_before_taxes doc)))))

(defn gross-total
  "Gets the gross total for the document."
  [doc]
  (+ (:total_before_taxes doc) (:total_taxes doc)))

(defn retention
  "Gets the total retention for the given document."
  [doc]
  (*
   (/ (:retention doc) 100)
   (:total_before_taxes doc)))

(defn client
  "Gets the client from the cache."
  [cache doc]
  (first (get-in cache [:clients [(:client_id doc) (:client_version doc)]])))

(defn customer-id
  "Gets the customer id for the given document."
  [cache doc]
  (if-let [client (client cache doc)]
    (if (or (nil? (:fiscal_id client)) (empty? (:fiscal_id client)))
      0
      (:id client))
    0))

(defn owner-invoice-number
  "Gets the document number for the owner document."
  [cache account doc]
  (let [owner-invoice-id (:owner_invoice_id doc)]
    (cond
      (or (nil? owner-invoice-id)
          (= owner-invoice-id (:id doc)))
        nil

      (some? (:raw_owner_invoice doc))
        (:raw_owner_invoice doc)

      :else
        (let [owner-invoice (first (get-in cache [:owner-documents owner-invoice-id]))]
          (assert owner-invoice (str "No document for owner-invoice-id " owner-invoice-id " - invoice " (:id doc)))
          (cond
            (common/guide? (:type owner-invoice))
              (guide-number cache account owner-invoice)
            :else
              (number cache account owner-invoice))))))

(defn document-xml
  "Converts the document in SAFT-T XML."
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

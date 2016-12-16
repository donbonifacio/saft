(ns saft.payment
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.account :as account]
    [saft.document :as document]
    [saft.payment-method :as payment-method]
    [saft.payment-item :as payment-item]
    [saft.item :as item]))

(defn receipts-query
  [{:keys [db account-id account begin end]}]
  (common/query-time-info "[SQL] Fetch payments"
     (j/query db [(str "select id, sequence_number,
                          document_number, document_serie,
                          retention,
                          total, total_taxes, total_before_taxes,
                          account_id, account_version, saft_hash,
                          created_at, updated_at, final_date, date,
                          client_id, client_version, owner_invoice_id,
                          tax_exemption_message
                        from invoices
                        where account_id = " account-id "
                          and type in ('Receipt')
                          and status in (" (common/saft-status-str)  ")
                          and (invoices.date between '" begin "' and '" end "')
                        order by invoices.id asc;")])))

(defn payment-type [account-version]
  (if (:iva_caixa account-version)
    "RC"
    "RG"))

(defn receipt-number [account-version doc]
  (str (payment-type account-version)
       " "
       (:document_serie doc)
       "/"
       (:document_number doc)))

(defn gross-total [owner-invoice doc]
  (+ (:total_taxes owner-invoice) (:total_before_taxes doc)))

(defn payment-xml
  [cache account doc]
  (let [account-version (account/for-document cache account doc)
        owner-invoice (first (get-in cache [:owner-documents (:owner_invoice_id doc)]))
        payment-methods (get-in cache [:payment-methods (:id doc)])
        payment-items (get-in cache [:payment-items (:id doc)])]
    (xml/element :Payment {}
                 (xml/element :PaymentRefNo {} (receipt-number account-version doc))
                 (xml/element :TransactionDate {} (common/get-date doc :date))
                 (xml/element :PaymentType {} (payment-type account-version))
                 (xml/element :SystemID {} (:id doc))
                 (xml/element :DocumentStatus {}
                              (xml/element :PaymentStatus {} (document/invoice-status doc))
                              (xml/element :PaymentStatusDate {} (document/final-date doc))
                              (xml/element :SourceID {} (:id account))
                              (xml/element :SourcePayment {} "P"))
                 (map payment-method/payment-method-xml payment-methods)
                 (xml/element :SourceID {} (:id account))
                 (xml/element :SystemEntryDate {} (document/final-date doc))
                 (xml/element :CustomerID {} (document/customer-id cache doc))
                 (map-indexed #(payment-item/payment-item-xml %1 %2 cache) payment-items)
                 (xml/element :DocumentTotals {}
                              (xml/element :TaxPayable {} (document/total-taxes doc))
                              (xml/element :NetTotal {} (:total_before_taxes doc))
                              (xml/element :GrossTotal {} (gross-total owner-invoice doc)))

                 )
                 )
    )

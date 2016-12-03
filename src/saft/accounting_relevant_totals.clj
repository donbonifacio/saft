(ns saft.accounting-relevant-totals
  (:require
    [clojure.java.jdbc :as j]))

(defn debit-documents [account]
  ["Invoice" "CashInvoice" "DebitNote" "InvoiceReceipt" "SimplifiedInvoice"])

(defn credit-documents [account]
  ["CreditNote"])

(defn statuses-relevant-for-communication []
  ["sent" "settled" "second_copy" "canceled"])

(defn invoice? [doc-type]
  (= "Invoice" doc-type))

(defn db-string-coll [coll]
  (->> coll
       (remove invoice?)
       (map #(str "'" %  "'"))))

(defn saft-status-str
  []
  (clojure.string/join ","
    (db-string-coll (statuses-relevant-for-communication))))

(defn types-condition [types]
  (let [db-types (db-string-coll types)]
    (str "(" (when (first (filter invoice? types))
                "invoices.type is null or ")
         "invoices.type in (" (clojure.string/join "," db-types) "))")))

(defn saft-types-condition [account]
  (types-condition
     (concat (debit-documents account)
             (credit-documents account))))

(defn run
  [db {:keys [begin end account]}]
  (let [not_canceled (str "(invoices.status <> 'canceled')")
        belongs_to_debit_type (types-condition (debit-documents account))
        belongs_to_credit_type (types-condition (credit-documents account))
        sql (str "select count(invoices.id) as number_of_entries,"
                   "sum(if(" not_canceled " and " belongs_to_credit_type ","
                                  "invoices.total_before_taxes,"
                                  "0.0)) as total_debit,"
                   "sum(if(" not_canceled " and " belongs_to_debit_type ","
                                  "invoices.total_before_taxes,"
                                  "0.0)) as total_credit "
                 "from invoices "
                 "where invoices.account_reset_id is null "
                    "and invoices.account_id = " (:id account) " "
                    "and (invoices.status in (" (saft-status-str) ")) "
                    "and " (saft-types-condition account) " "
                    "and (invoices.date between '" begin "' and '" end "');")]
    (first (j/query db [sql]))))


(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.accounting-relevant-totals
  "Loads and writes accountint relevant totals."
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn run
  "Performs que query to obtain the accounting relevant totals."
  [db {:keys [begin end account]}]
  (common/query-time-info "[SQL] Accouting relevant totals query"
    (let [not_canceled (str "(invoices.status <> 'canceled')")
          belongs_to_debit_type (common/types-condition (common/debit-documents account))
          belongs_to_credit_type (common/types-condition (common/credit-documents account))
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
                      "and (invoices.status in (" (common/saft-status-str) ")) "
                      "and " (common/saft-types-condition account) " "
                      "and (invoices.date between '" begin "' and '" end "');")
          result (first (j/query db [sql]))]
      result)))

(defn totals-xml
  "Given the totals, writes the xml for them."
  [totals]
  [(xml/element :NumberOfEntries {} (:number_of_entries totals))
   (xml/element :TotalDebit {} (:total_debit totals))
   (xml/element :TotalCredit {} (:total_credit totals))])

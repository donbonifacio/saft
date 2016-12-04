(ns saft.accounting-relevant-totals
  (:require
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn run
  [db {:keys [begin end account]}]
  (common/time-info "[SQL] Accouting relevant totals query"
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
                      "and (invoices.date between '" begin "' and '" end "');")]
      (first (j/query db [sql])))))


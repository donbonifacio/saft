(ns saft.payment-totals
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn run
  [db {:keys [begin end account]}]
  (common/query-time-info "[SQL] Payment totals query"
    (let [not_canceled (str "(invoices.status <> 'canceled')")
          belongs_to_debit_type (common/types-condition (common/debit-documents account))
          belongs_to_credit_type (common/types-condition (common/credit-documents account))
          sql (str "select count(distinct invoices.id) as number_of_entries,
                           sum(if(" not_canceled " and receipt_datas.paid > 0,
                                  receipt_datas.tax,
                                  0.0)) as total_credit,
                           sum(if("not_canceled" and receipt_datas.paid < 0,
                                  receipt_datas.tax,
                                  0.0))*-1 as total_debit
                    from invoices
                    inner join receipt_datas on invoices.id = receipt_datas.receipt_id
                    where invoices.account_reset_id is null
                          and invoices.account_id = " (:id account) "
                          and (invoices.status in ("(common/saft-status-str)"))
                          and type = 'Receipt'
                          and (invoices.date between '" begin "' and '" end "');")

          result (first (j/query db [sql]))]
      result)))

(defn totals-xml [totals]
  [(xml/element :NumberOfEntries {} (:number_of_entries totals))
   (xml/element :TotalDebit {} (:total_debit totals))
   (xml/element :TotalCredit {} (:total_credit totals))])

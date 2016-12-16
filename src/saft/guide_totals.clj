(ns saft.guide-totals
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn run
  [db {:keys [begin end account]}]
  (common/query-time-info "[SQL] Guide totals query"
    (let [not_canceled (str "(invoices.status <> 'canceled')")
          belongs_to_debit_type (common/types-condition (common/debit-documents account))
          belongs_to_credit_type (common/types-condition (common/credit-documents account))
          sql (str " select
                       count(invoice_items.id) as line_count,
                       sum(invoice_items.quantity) as quantity
                       from invoices
                           inner join clients on (invoices.client_id = clients.id)
                       inner join invoice_items on (invoices.id = invoice_items.invoice_id)
                       inner join addresses as address_to on (invoices.address_to_id = address_to.id)
                       inner join addresses as address_from on (invoices.address_from_id = address_from.id)
                       inner join tax_countries as tax_location_to on (tax_location_to.id = address_to.tax_location_id)
                       inner join tax_countries as tax_location_from on (tax_location_from.id = address_from.tax_location_id)
                       where invoices.account_reset_id is null
                             and invoices.account_id = " (:id account) "
                             and (invoices.status in ("(common/saft-status-str)"))
                             and " (common/saft-guides-condition account) "
                             and (invoices.date between '" begin "' and '" end "')
                             and tax_location_from.name = 'Portugal'
                             and tax_location_to.name = 'Portugal'
                             and clients.country = 'Portugal'")

          result (first (j/query db [sql]))]
      result)))

(defn totals-xml [totals]
  [(xml/element :NumberOfMovementLines {} (:line_count totals))
   (xml/element :TotalQuantityIssued {} (:quantity totals))])

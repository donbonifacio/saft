(ns saft.tax-table
  (:require
    [clojure.java.jdbc :as j]
    [clojure.data.xml :as xml]
    [saft.common :as common]))

(defn run
  [db {:keys [begin end account]}]
  (common/do-query db "[SQL] Fetch tax table"
    (str "select distinct taxes.id, taxes.name, region, value "
          "from taxes "
             "inner join invoice_items on (invoice_items.tax_id = taxes.id) "
             "inner join invoices on (invoices.id = invoice_items.invoice_id) "
            "where invoices.account_id = " (:id account) " "
             "and invoices.account_reset_id is null "
            "and (invoices.status in (" (common/saft-status-str) ")) "
            "and " (common/saft-types-condition account) " "
            "and (invoices.date between '" begin "' and '" end "');")))

(defn region [tax]
  (let [region (:region tax)]
    (if (and (some? region) (not= region "Desconhecido"))
      (clojure.string/replace region #" " "")
      "PT")))

(defn code [value]
  (cond
    (nil? value) "ISE"
    (== 0 value) "ISE"
    (== 23.0 value) "NOR"
    (== 13.0 value) "INT"
    (== 6.0 value) "RED"
    :else "OUT"))

(defn tax-table-entry-xml
  [tax]
  (xml/element :TaxTableEntry {}
               (xml/element :TaxType {} "IVA")
               (xml/element :TaxCountryRegion {} (region tax))
               (xml/element :TaxCode {} (code (:value tax)))
               (xml/element :Description {} (:name tax))
               (xml/element :TaxPercentage {} (:value tax))))

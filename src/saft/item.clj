(ns saft.item
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn items-query
  [{:keys [db]} doc-ids]
  (if-let [doc-ids (seq doc-ids)]
    (common/time-info (str "[SQL] Fetch items for " (count doc-ids) " document(s)")
               (j/query db [(str
                              "select id, invoice_id, name, description, quantity, unit_price
                              from invoice_items
                              where invoice_id in (" (clojure.string/join "," doc-ids) ")")]))
    []))

(defn item-xml [idx item]
  (xml/element :Line {}
               (xml/element :LineNumber {} (inc idx))
               (xml/element :ProductCode {} (common/get-str item :name))
               (xml/element :ProductDescription {} (common/get-str item :description))
               (xml/element :Quantity {} (:quantity item))
               (xml/element :UnitOfMeasure {} (:unit item))
               (xml/element :UnitPrice {} (:unit_price item))
               (xml/element :TaxPointDate {} (:tax_point_date item))
               (xml/element :Description {} (common/get-str item :description))
               (xml/element :CreditAmount {} (:credit item))
               (xml/element :Tax {}
                            (xml/element :TaxType {} "IVA")
                            (xml/element :TaxCountryRegion {} "PT")
                            (xml/element :TaxCode {} "NOR")
                            (xml/element :TaxPercentage {} "23.0"))))


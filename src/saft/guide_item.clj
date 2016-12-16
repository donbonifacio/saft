(ns saft.guide-item
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.document :as document]
    [saft.item :as item]
    [saft.tax-table :as tax-table]
    [saft.common :as common]))

(defn guide-items-query
  [{:keys [db]} doc-ids]
  (if-let [doc-ids (seq doc-ids)]
    (common/query-time-info (str "[SQL] Fetch guide items for " (count doc-ids) " document(s)")
               (j/query db [(str
                              "select id, invoice_id, name, description,
                              quantity, unit, unit_price, subtotal, tax_value,
                              discount_amount, tax_region, product_id
                              from invoice_items
                              where invoice_id in (" (clojure.string/join "," doc-ids) ")")]))
    []))

(defn guide-item-xml [idx client doc item cache]
  (let [account (:account cache)]
    (xml/element :Line {}
                 (xml/element :LineNumber {} (inc idx))
                 (xml/element :ProductCode {} (:product_id item))
                 (xml/element :ProductDescription {} (common/get-str item :name))
                 (xml/element :Quantity {} (:quantity item))
                 (xml/element :UnitOfMeasure {} (or (:unit item) "unit"))
                 (xml/element :UnitPrice {} (:unit_price item))
                 (xml/element :Description {} (item/description item))
                 (if (= "Devolution" (:type doc))
                   (xml/element :DebitAmount {} (:subtotal item))
                   (xml/element :CreditAmount {} (:subtotal item)))
                 (xml/element :Tax {}
                            (xml/element :TaxType {} "IVA")
                            (xml/element :TaxCountryRegion {} (item/tax-region client item))
                            (xml/element :TaxCode {} (tax-table/code (:tax_value item)))
                            (xml/element :TaxPercentage {} (:tax_value item)))
                 )))

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
                              "select id, invoice_id, name, description,
                              quantity, unit, unit_price, subtotal
                              from invoice_items
                              where invoice_id in (" (clojure.string/join "," doc-ids) ")")]))
    []))

(defn description [product]
  (if (or (nil? (:description product)) (empty? (:description product)))
    (common/get-str product :name 199)
    (common/get-str product :description 199)))

(defn item-xml [idx doc item]
  (xml/element :Line {}
               (xml/element :LineNumber {} (inc idx))
               (xml/element :ProductCode {} (common/get-str item :name))
               (xml/element :ProductDescription {} (description item))
               (xml/element :Quantity {} (:quantity item))
               (xml/element :UnitOfMeasure {} (or (:unit item) "unit"))
               (xml/element :UnitPrice {} (:unit_price item))
               (xml/element :TaxPointDate {} (common/get-date doc :date))
               (xml/element :Description {} (description item))
               (if (= "CreditNote" (:type doc))
                 (xml/element :DebitAmount {} (:subtotal item))
                 (xml/element :CreditAmount {} (:subtotal item)))
               (xml/element :Tax {}
                            (xml/element :TaxType {} "IVA")
                            (xml/element :TaxCountryRegion {} "PT")
                            (xml/element :TaxCode {} "NOR")
                            (xml/element :TaxPercentage {} "23.0"))))


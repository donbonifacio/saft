(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.item
  "Loads and generates document items information."
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.tax-table :as tax-table]
    [saft.countries :as countries]))

(defn items-query
  "Loads all the items for the given document ids."
  [{:keys [db]} doc-ids]
  (if-let [doc-ids (seq doc-ids)]
    (common/query-time-info (str "[SQL] Fetch items for " (count doc-ids) " document(s)")
               (j/query db [(str
                              "select id, invoice_id, name, description,
                              quantity, unit, unit_price, subtotal, tax_value,
                              discount_amount, tax_region
                              from invoice_items
                              where invoice_id in (" (clojure.string/join "," doc-ids) ")")]))
    []))

(defn description
  "Gets the description for the given product."
  [product]
  (if (or (nil? (:description product)) (empty? (:description product)))
    (common/get-str product :name 199)
    (common/get-str product :description 199)))

(defn exempt?
  "True if an item is exempt from taxes."
  [item]
  (and (some? (:tax_value item)) (zero? (:tax_value item))))

(defn tax-exemption-reason
  "Returns the tax exemption reason for the given document."
  [doc]
  (if (:tax_exemption_message doc)
    (:tax_exemption_message doc)
    "Não sujeito; não tributado (ou similar)."))

(defn discount-applied?
  "True if the given item has an applied discount."
  [item]
  (and (some? (:discount_amount item))
       (pos? (:discount_amount item))))

(defn settlement-amount
  "Returns the settlement amount for the given item."
  [item]
  (:discount_amount item))

(defn tax-region-from-item
  "Returns the tax region from the item."
  [item]
  (if (nil? (:tax_region item))
    "PT"
    (clojure.string/replace (:tax_region item) #"\s" "")))

(defn tax-region
  "Gets the tax region for the item, considering the item region."
  [client item]
  (let [region (tax-region-from-item item)]
    (if (not= "Desconhecido" region)
      region
      (countries/country-code (:country client)))))

(defn item-xml
  "Generates XML for the given item."
  [idx client doc owner-invoice-number item]
  (xml/element :Line {}
               (xml/element :LineNumber {} (inc idx))
               (xml/element :ProductCode {} (common/get-str item :name))
               (xml/element :ProductDescription {} (description item))
               (xml/element :Quantity {} (:quantity item))
               (xml/element :UnitOfMeasure {} (or (:unit item) "unit"))
               (xml/element :UnitPrice {} (:unit_price item))
               (xml/element :TaxPointDate {} (common/get-date doc :date))
               (when owner-invoice-number
                 (xml/element :References {}
                   (xml/element :Reference {} owner-invoice-number)))
               (xml/element :Description {} (description item))
               (if (= "CreditNote" (:type doc))
                 (xml/element :DebitAmount {} (:subtotal item))
                 (xml/element :CreditAmount {} (:subtotal item)))
               (xml/element :Tax {}
                            (xml/element :TaxType {} "IVA")
                            (xml/element :TaxCountryRegion {} (tax-region client item))
                            (xml/element :TaxCode {} (tax-table/code (:tax_value item)))
                            (xml/element :TaxPercentage {} (:tax_value item)))
               (when (exempt? item)
                 (xml/element :TaxExemptionReason {} (tax-exemption-reason doc)))
               (when (discount-applied? item)
                 (xml/element :SettlementAmount {} (settlement-amount item)))))

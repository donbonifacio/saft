(ns saft.product
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn products-query
  [{:keys [db account-id account begin end]}]
  (common/time-info "[SQL] Fetch products"
    (j/query db [(str "select distinct products.id,
                         products.description, products.name, products.unit
                       from products
                         inner join invoice_items on (products.id = invoice_items.product_id)
                         inner join invoices on (invoices.id = invoice_items.invoice_id)
                       where invoices.account_reset_id is null
                         and invoices.account_id = ?
                         and " (common/saft-types-condition account) "
                         and status in (" (common/saft-status-str)  ")
                         and (invoices.date between '" begin "' and '" end "')
                       order by products.id asc")
                 account-id])))

(defn description [product]
  (if (or (nil? (:description product)) (empty? (:description product)))
    (common/get-str product :name 199)
    (common/get-str product :description 199)))

(defn product-code [product]
  (let [unit (:unit product)]
    (case unit
      "hour" "S"
      "day" "S"
      "month" "S"
      "unit" "P"
      "other" "O"
      "service" "S"
      "S")))

(defn product-xml [product]
  (xml/element :Product {}
               (xml/element :ProductType {} (product-code product))
               (xml/element :ProductCode {} (common/get-str product :name 59))
               (xml/element :ProductDescription {} (description product))
               (xml/element :ProductNumberCode {} (common/get-str product :name 49))))

(defn products-xml [products]
  (map product-xml products))


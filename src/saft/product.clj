(ns saft.product
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn products-query
  [{:keys [db account-id account begin end]}]
  (common/time-info "[SQL] Fetch products"
    (j/query db [(str "select distinct products.id,
                         products.description, products.name
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

(defn product-xml [product]
  (xml/element :Product {}
               (xml/element :ProductType {} "S")
               (xml/element :ProductCode {} (common/get-str product :name))
               (xml/element :ProductDescription {} (common/get-str product :description))
               (xml/element :ProductNumberCode {} (common/get-str product :name))))

(defn products-xml [products]
  (map product-xml products))


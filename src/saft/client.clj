(ns saft.client
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.countries :as countries]))

(defn clients-query
  [{:keys [db account-id account begin end]}]
  (common/time-info "[SQL] Fetch clients"
     (j/query db [(str "select distinct client_versions.id,
                          name, fiscal_id, email, website,
                          address, postal_code, country, phone, fax
                        from client_versions
                        inner join invoices on (
                          invoices.client_id = client_versions.client_id
                          and client_versions.version = invoices.client_version)
                        where invoices.account_id = ?
                          and " (common/all-types-condition account) "
                          and status in (" (common/saft-status-str)  ")
                          and invoices.account_reset_id is null
                        order by client_versions.id asc")
                  account-id])))

(defn final-consumer?
  "True if the client is final consumer"
  [client]
  (and (pos? (:id client))
       (or (nil? (:fiscal_id client))
           (empty? (:fiscal_id client)))))

(defn fiscal-id [client]
  (if (nil? (:fiscal_id client))
    "999999990"
    (clojure.string/replace (:fiscal_id client) #"\s" "")))

(defn portuguese? [client]
  (or (= "Portugal" (:country client))
      (nil? (:country client))))

(defn postal-code [client]
  (if (portuguese? client)
    (common/get-str client :postal_code 8 "0000-000")
    (common/get-str client :postal_code 19 "0000-000")))

(defn client-xml [client]
  (when-not (final-consumer? client)
    (xml/element :Customer {}
                 (xml/element :CustomerID {} (:id client))
                 (xml/element :AccountID {} "Desconhecido")
                 (xml/element :CustomerTaxID {} (fiscal-id client))
                 (xml/element :CompanyName {} (:name client))
                 (xml/element :BillingAddress {}
                              (xml/element :AddressDetail {} (common/get-str client :address 59))
                              (xml/element :City {} (common/get-str client :city 99))
                              (xml/element :PostalCode {} (postal-code client))
                              (xml/element :Country {} (countries/country-code (:country client))))
                 (xml/element :Telephone {} (common/get-str client :phone 19))
                 (xml/element :Fax {} (common/get-str client :fax 19))
                 (xml/element :Email {} (common/get-str client :email 59))
                 (xml/element :Website {} (common/get-str client :website 59))
                 (xml/element :SelfBillingIndicator {} "0"))))

(def final-consumer
  {:id 0
   :name "Consumidor Final"})

(defn clients-xml [clients]
  (map client-xml (concat clients [final-consumer])))

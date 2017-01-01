(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.client
  "Loads clients and writes client's XML SAF-T."
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]
    [saft.countries :as countries]))

(def final-consumer
  "The final consumer client"
  {:id 0
   :name "Consumidor Final"})

(def final-consumer-fiscal-id
  "The final consumer constant fiscal id"
  "999999990")

(defn clients-query
  "Gets all the client versions for an account."
  [{:keys [db account-id account begin end]}]
  (common/query-time-info "[SQL] Fetch clients"
     (j/query db [(str "select distinct client_versions.id,
                          name, fiscal_id, email, website,
                          address, postal_code, country, phone, fax,
                          version, client_versions.client_id
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

(defn clients-query-by-documents
  "Gets all the client versions referenced by the given documents."
  [{:keys [db account-id]} documents]
  (common/query-time-info (str "[SQL] Fetch clients for " (count documents) " documents ")
    (if-let [doc-ids (->> (keep :id documents)
                          (distinct)
                          (seq))]
      (j/query db [(str "select distinct client_versions.id,
                          name, fiscal_id, email, website,
                          address, postal_code, country, phone, fax,
                          version, client_versions.client_id
                        from client_versions
                        inner join invoices on (
                          invoices.client_id = client_versions.client_id
                          and client_versions.version = invoices.client_version)
                        where invoices.id in (" (clojure.string/join "," doc-ids)  ")
                        order by client_versions.id asc")])
      [])))

(defn final-consumer?
  "True if the client is final consumer."
  [client]
  (and (pos? (:id client))
       (or (nil? (:fiscal_id client))
           (empty? (:fiscal_id client)))))

(defn fiscal-id
  "Gets the fiscal id for the client, or the default one for final consumer."
  [client]
  (if (or (nil? (:fiscal_id client))
          (empty? (:fiscal_id client)))
    final-consumer-fiscal-id
    (clojure.string/replace (:fiscal_id client) #"\s" "")))

(defn portuguese?
  "True if the client is from Portugal or country is not specified."
  [client]
  (or (= "Portugal" (:country client))
      (nil? (:country client))))

(defn postal-code
  "Returns the properly formatted postal_code for the client"
  [client]
  (if (portuguese? client)
    (common/get-str client :postal_code 8 "0000-000")
    (common/get-str client :postal_code 19 "0000-000")))

(defn client-xml
  "Returns a client as SAF-T XML."
  [client]
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

(defn clients-xml
  "Returns the XML for clients in SAF-T format."
  [clients]
  (map client-xml (concat clients [final-consumer])))

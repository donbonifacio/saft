(ns saft.client
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn clients-query
  [{:keys [db account-id account begin end]}]
  (common/time-info "[SQL] Fetch clients"
     (j/query db [(str "select distinct client_versions.id,
                          name, fiscal_id
                        from client_versions
                        inner join invoices on (
                          invoices.client_id = client_versions.client_id
                          and client_versions.version = invoices.client_version)
                        where invoices.account_id = ?
                          and " (common/saft-types-condition account) "
                          and status in (" (common/saft-status-str)  ")
                          and (invoices.date between '" begin "' and '" end "')
                          and invoices.account_reset_id is null
                        order by client_versions.id asc")
                  account-id])))

(defn client-xml [client]
  (xml/element :Customer {}
               (xml/element :CustomerID {} (:id client))
               (xml/element :AccountID {} "Desconhecido")
               (xml/element :CustomerTaxID {} (:fiscal_id client))
               (xml/element :CompanyName {} (:name client))
               (xml/element :BillingAddress {}
                            (xml/element :AddressDetail {} "Desconhecido")
                            (xml/element :City {} "Desconhecido")
                            (xml/element :PostalCode {} "0000-000")
                            (xml/element :Country {} "PT"))
               (xml/element :Telephone {} "Desconhecido")
               (xml/element :Fax {} "Desconhecido")
               (xml/element :Email {} "Desconhecido")
               (xml/element :Website {} "Desconhecido")
               (xml/element :SelfBillingIndicator {} "0")))

(defn clients-xml [clients]
  (map client-xml clients))

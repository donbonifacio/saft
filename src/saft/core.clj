(ns saft.core
  (:require [clojure.data.xml :as xml]
            [clojure.java.jdbc :as j]))

(def db {:classname "com.mysql.jdbc.Driver" 
         :subprotocol "mysql"
         :subname "//localhost:3306/invoicexpress"
         :user "root"})

#_(println (first (j/query db ["select organization_name, fiscal_id, "
                                  "email, "
                                  "address, postal_code, city "
                             "from accounts where id = ?" 5554])))

(defn- fetch-all-data
  "Gets all needed data from storage"
  [account-id]
  {:account (first (j/query db ["select * from accounts where id = ?" account-id]))

   :clients (j/query db [" select distinct client_versions.*
                         from client_versions
                         inner join invoices on (
                         invoices.client_id = client_versions.client_id 
                         and client_versions.version = invoices.client_version)
                         where invoices.account_id = ?
                         and invoices.account_reset_id is null" account-id])

   :products (j/query db ["select distinct products.*
                          from products
                          inner join invoice_items on (products.id = invoice_items.product_id)
                          inner join invoices on (invoices.id = invoice_items.invoice_id)
                          where invoices.account_reset_id is null
                          and invoices.account_id = ?
                          " account-id])

   :documents (j/query db ["select * from invoices where account_id = ?" account-id])})

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

(defn- write-clients [clients]
  (map client-xml clients))

(defn- write-products [products]
  (map (fn [product]
         (xml/element :Product {}
                      (xml/element :ProductType {} "S")
                      (xml/element :ProductCode {} (:id product))))
       products))

(defn- write-invoice-items [data doc]
  (map (fn [item]
         (xml/element :Line {}
                      (xml/element :Quantity {} (:quantity item))))
       (j/query db ["select * from invoice_items where invoice_id = ?" (:id doc)])))

(defn- write-documents [data docs]
  (map (fn [doc]
         (xml/element :SalesInvoices {}
                      (xml/element :InvoiceNo {} (str (:sequence_number doc)))
                      (xml/element :TaxPayable {} (:total doc))
                      (write-invoice-items data doc)))
       docs))

(defn header-xml [args account]
  (xml/element :Header {}
               (xml/element :AuditFileVersion {} "1.03_01")
               (xml/element :CompanyID {} (:fiscal_id account))
               (xml/element :TaxRegistrationNumber {} (:fiscal_id account))
               (xml/element :TaxAccountingBasis {} "F")
               (xml/element :CompanyName {} (:organization_name account))
               (xml/element :CompanyAddress {}
                            (xml/element :AddressDetail {} (:address account))
                            (xml/element :City {} (:city account))
                            (xml/element :PostalCode {} (:postal_code account))
                            (xml/element :Country {} "PT"))
               (xml/element :FiscalYear {} (:year args))
               (xml/element :StartDate {} (:start-date args))
               (xml/element :EndDate {} (:end-date args))
               (xml/element :CurrencyCode {} "EUR")
               (xml/element :DateCreated {} (:created args))
               (xml/element :TaxEntity {} "Global")
               (xml/element :ProductCompanyTaxID {} "508025338")
               (xml/element :SoftwareCertificateNumber {})
               (xml/element :ProductID {})
               (xml/element :ProductVersion {} "1.0")

               ))

(defn- write-saft [data account]
  (xml/element :AuditFile {:xmlns "urn:OECD:StandardAuditFile-Tax:PT_1.03_01"
                           "xmlns:xsi" "http://www.w3.org/2001/XMLSchema-instance"}
               (header-xml {} account)
               (xml/element :MasterFiles {}
                            (write-clients (:clients data))
                            (write-products (:products data)))
               (xml/element :SourceDocuments {}
                            (write-documents data (:documents data)))))


(defn ppxml [xml]
  (let [in (javax.xml.transform.stream.StreamSource.
             (java.io.StringReader. xml))
        writer (java.io.StringWriter.)
        out (javax.xml.transform.stream.StreamResult. writer)
        transformer (.newTransformer 
                      (javax.xml.transform.TransformerFactory/newInstance))]
    (.setOutputProperty transformer 
                        javax.xml.transform.OutputKeys/INDENT "yes")
    (.setOutputProperty transformer 
                        "{http://xml.apache.org/xslt}indent-amount" "2")
    (.setOutputProperty transformer 
                        javax.xml.transform.OutputKeys/METHOD "xml")
    (.transform transformer in out)
    (-> out .getWriter .toString)))

(defn foo
  []
  (time
    (let [account-id 5554
          ;account-id 6599
          data (fetch-all-data account-id)
          file-name "/tmp/foo.xml"
          account (:account data)
          tags (write-saft data account)]
      (println "Account-id:" account-id)
      (println "Docs:" (count (:documents data)))
      (with-open [out-file (java.io.OutputStreamWriter. (java.io.FileOutputStream. file-name) "UTF-8")]
        (xml/emit tags out-file))
      (let [raw (slurp file-name)]
        (println (ppxml raw))))))

(ns saft.core
  (:require [clojure.data.xml :as xml]
            [clojure.java.jdbc :as j]
            [saft.accounting-relevant-totals :as accounting-relevant-totals]))

(def db {:classname "com.mysql.jdbc.Driver" 
         :subprotocol "mysql"
         :subname "//localhost:3306/invoicexpress"
         :user "root"})

(defmacro time-info
  [info expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println (str ~info ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))

#_(println (accounting-relevant-totals/run db {:begin "2004-01-01"
                                               :end "2024-01-01 "
                                               :account {:id 5554}}))

#_(println (first (j/query db ["select organization_name, fiscal_id, "
                               "email, "
                               "address, postal_code, city "
                               "from accounts where id = ?" 5554])))

#_(println (first (j/query db ["select distinct products.*
                               from products
                               inner join invoice_items on (products.id = invoice_items.product_id)
                               inner join invoices on (invoices.id = invoice_items.invoice_id)
                               where invoices.account_reset_id is null
                               and invoices.account_id = ?
                               " 5554])))

(defn- fetch-all-data
  "Gets all needed data from storage"
  [account-id]
  {:account (first (j/query db ["select * from accounts where id = ?" account-id]))

   :clients (j/query db [" select distinct client_versions.id,
                         name, fiscal_id
                         from client_versions
                         inner join invoices on (
                         invoices.client_id = client_versions.client_id 
                         and client_versions.version = invoices.client_version)
                         where invoices.account_id = ?
                         and invoices.account_reset_id is null" account-id])

   :products (j/query db ["select distinct products.id,
                          products.description, products.name
                          from products
                          inner join invoice_items on (products.id = invoice_items.product_id)
                          inner join invoices on (invoices.id = invoice_items.invoice_id)
                          where invoices.account_reset_id is null
                          and invoices.account_id = ?
                          " account-id])

   :documents (j/query db ["select id, sequence_number
                           from invoices
                           where account_id = ? and status <> 'draft'"
                           account-id])})

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

(defn get-str [m k]
  (apply str (filter #(<= 32 (int %) 126) (get m k ""))))

(defn get-date [m k]
  (str (get m k)))

(defn product-xml [product]
  (xml/element :Product {}
               (xml/element :ProductType {} "S")
               (xml/element :ProductCode {} (get-str product :name))
               (xml/element :ProductDescription {} (get-str product :description))
               (xml/element :ProductNumberCode {} (get-str product :name))))

(defn- write-products [products]
  (map product-xml products))

(defn item-xml [idx item]
  (xml/element :Line {}
               (xml/element :LineNumber {} (inc idx))
               (xml/element :ProductCode {} (get-str item :name))
               (xml/element :ProductDescription {} (get-str item :description))
               (xml/element :Quantity {} (:quantity item))
               (xml/element :UnitOfMeasure {} (:unit item))
               (xml/element :UnitPrice {} (:unit_price item))
               (xml/element :TaxPointDate {} (:tax_point_date item))
               (xml/element :Description {} (get-str item :description))
               (xml/element :CreditAmount {} (:credit item))
               (xml/element :Tax {}
                            (xml/element :TaxType {} "IVA")
                            (xml/element :TaxCountryRegion {} "PT")
                            (xml/element :TaxCode {} "NOR")
                            (xml/element :TaxPercentage {} "23.0"))))

(defn fetch-items [doc-ids]
  (j/query db [(str
                 "select id, invoice_id, name, description, quantity, unit_price
                 from invoice_items
                 where invoice_id in (" (clojure.string/join "," doc-ids) ")")]))

(defn prepare-items [cache account doc]
  (cond
    (some? (:items doc)) doc
    (some? (get cache (:id doc))) (assoc doc :items (get cache :id doc))
    :else (assoc doc :items (fetch-items [(:id doc)]))))

(defn invoice-xml [cache account doc]
  (let [doc (prepare-items cache account doc)]
    (xml/element :Invoice {}
                 (xml/element :InvoiceNo {} (str (:sequence_number doc)))
                 (xml/element :DocumentStatus {}
                              (xml/element :InvoiceStatus {} "A")
                              (xml/element :InvoiceStatusDate {} "2016-07-01T15:06:33")
                              (xml/element :SourceID {} (:id account))
                              (xml/element :SourceBilling {} "P"))
                 (xml/element :Hash {} (:saft_hash doc))
                 (xml/element :HashControl {} 1)
                 (xml/element :Period {} 1)
                 (xml/element :InvoiceDate {} (get-date doc :date))
                 (xml/element :InvoiceType {} "FT")
                 (xml/element :SpecialRegimes {}
                              (xml/element :SelfBillingIndicator {} 0)
                              (xml/element :CashVATSchemeIndicator {} 0)
                              (xml/element :ThirdPartiesBillingIndicator {} 0))
                 (xml/element :SourceID {} (:id account))
                 (xml/element :SystemEntryDate {} (get-date doc :created_at))
                 (xml/element :CustomerID {} 0)
                 (map-indexed item-xml (:items doc))
                 (xml/element :DocumentTotals {}
                              (xml/element :TaxPayable {} (:tax doc))
                              (xml/element :NetTotal {} (:total doc))
                              (xml/element :GrossTotal {} (:total_with_taxes doc))))))

(defn preload-docs [data docs]
  (let [doc-ids (map :id docs)
        items (fetch-items doc-ids)]
    (group-by :invoice_id items)))

(defn- write-documents [data docs]
  (let [cache {}
        cache (preload-docs data docs)
        totals (time-info "Accouting relevant totals query"
                 (accounting-relevant-totals/run db (merge data
                                                       {:begin "2004-01-01"
                                                        :end "2024-01-01"})))]
    (xml/element :SalesInvoices {}
                 (xml/element :NumberOfEntries {} (:number_of_entries totals))
                 (xml/element :TotalDebit {} (:total_debit totals))
                 (xml/element :TotalCredit {} (:total_credit totals))
                 (map #(invoice-xml cache (:account data) %) docs))))

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
               (xml/element :ProductVersion {} "1.0")))

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
  (time-info "Complete SAFT"
    (let [account-id 5554
          account-id 6599
          data (fetch-all-data account-id)
          file-name "/tmp/foo.xml"
          account (:account data)
          tags (time-info "Build XML structure" (write-saft data account))]
      (println "Account-id:" account-id)
      (println "Docs:" (count (:documents data)))
      (with-open [out-file (java.io.OutputStreamWriter. (java.io.FileOutputStream. file-name) "UTF-8")]
        (time-info "Write to file" (xml/emit tags out-file)))
      (let [raw (slurp file-name)
            formatted (time-info "Format XML" (ppxml raw))]
        (spit "tmp/formatted.xml" formatted)))))


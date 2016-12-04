(ns saft.core
  (:gen-class)
  (:require [clojure.data.xml :as xml]
            [clojure.java.jdbc :as j]
            [clojure.tools.cli :as cli]
            [saft.tax-table :as tax-table]
            [saft.common :as common]
            [saft.document :as document]
            [saft.accounting-relevant-totals :as accounting-relevant-totals]))

(def db {:classname "com.mysql.jdbc.Driver" 
         :subprotocol "mysql"
         :subname "//localhost:3306/invoicexpress"
         :user "root"})

(defn- fetch-all-data
  "Gets all needed data from storage"
  [{:keys [account-id begin end] :as args}]
  (let [account (common/time-info "[SQL] Fetch account"
                           (first (j/query db ["select * from accounts where id = ?" account-id])))
        args (assoc args :account account)]

    {:account account

     :clients (common/time-info "[SQL] Fetch clients"
                         (j/query db [(str " select distinct client_versions.id,
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
                                      account-id]))

     :products (common/time-info "[SQL] Fetch products"
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
                                       account-id]))

     :documents (document/documents-query args)}))

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

(defn product-xml [product]
  (xml/element :Product {}
               (xml/element :ProductType {} "S")
               (xml/element :ProductCode {} (common/get-str product :name))
               (xml/element :ProductDescription {} (common/get-str product :description))
               (xml/element :ProductNumberCode {} (common/get-str product :name))))

(defn- write-products [products]
  (map product-xml products))

(defn fetch-items [doc-ids]
  (if-let [doc-ids (seq doc-ids)]
    (common/time-info (str "[SQL] Fetch items for " (count doc-ids) " document(s)")
               (j/query db [(str
                              "select id, invoice_id, name, description, quantity, unit_price
                              from invoice_items
                              where invoice_id in (" (clojure.string/join "," doc-ids) ")")]))
    []))

(defn fetch-account-versions
  [{:keys [account] :as data} account-versions]
  (if-let [account-versions (->> account-versions
                                 (remove nil?)
                                 (distinct)
                                 (seq))]
    (common/time-info (str "[SQL] Fetch " (count account-versions) " account versions")
               (j/query db [(str
                              "select id, version, iva_caixa, factura_recibo
                              from account_versions
                              where account_id = " (:id account) "
                              and version in (" (clojure.string/join "," account-versions) ")")]))
    []))

(defn prepare-items [cache account doc]
  (cond
    (some? (:items doc)) doc
    (some? (get-in cache [:items (:id doc)])) (assoc doc :items (get-in cache [:items (:id doc)]))
    :else (assoc doc :items (fetch-items [(:id doc)]))))

(defn invoice-xml [cache account doc]
  (let [doc (prepare-items cache account doc)]
    (document/document-xml cache account doc)))

(defn preload-docs [data docs]
  (let [doc-ids (map :id docs)
        items (fetch-items doc-ids)]
    (group-by :invoice_id items)))

(defn preload-account-versions [data docs]
  (let [account-versions (map :account_version docs)
        versions (fetch-account-versions data account-versions)]
    (group-by :version versions)))

(defn- write-documents [data docs]
  (let [cache {}
        cache {:items (preload-docs data docs)
               :account_versions (preload-account-versions data docs)}
        totals (accounting-relevant-totals/run db data)]
    (println "Totals: " totals)
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

(defn write-tax-table [data]
  (let [tax-table (tax-table/run db data)]
    (xml/element :TaxTable {}
                 (map tax-table/tax-table-entry-xml tax-table))))

(defn- write-saft [data account]
  (xml/element :AuditFile {:xmlns "urn:OECD:StandardAuditFile-Tax:PT_1.03_01"
                           "xmlns:xsi" "http://www.w3.org/2001/XMLSchema-instance"}
               (header-xml {} account)
               (xml/element :MasterFiles {}
                            (write-clients (:clients data))
                            (write-products (:products data))
                            (write-tax-table data))
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

(defn generate-saft
  [{:keys [account-id output formatted] :as args}]
  (common/time-info (str "[ALL] Complete SAFT [" output "]")
     (do
       (println "---------------------")
       (println "SAF-T for Account-id:" account-id)
       (let [args (assoc args :db db)
             data (merge args (fetch-all-data args))
             account (:account data)
             tags (common/time-info "[XML] Build XML structure" (write-saft data account))]
         (with-open [out-file (java.io.OutputStreamWriter. (java.io.FileOutputStream. output) "UTF-8")]
           (common/time-info "[FILE] Write to file" (xml/emit tags out-file)))
         (let [raw (slurp output)
               formatted (common/time-info "[FILE] Format XML" (ppxml raw))]
           (spit output formatted))))))

(def cli-options
  [["-a" "--account-id ID" "Account ID"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   ["-o" "--output FILE" "Output file"]

   ["-f" "--formatted" "Format XML"]

   ["-y" "--year YEAR" "Year"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 2000 % 2020) "Must be a number between 2000 and 2020"]]

   ["-m" "--month MONTH" "Month"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 13) "Must be a number between 1 and 12"]]

   ["-h" "--help"]])

(defn -main [& args]
  (let [data (cli/parse-opts args cli-options)
        year (get-in data [:options :year] 2016)
        month (get-in data [:options :month] 1)
        output (get-in data [:options :output] "saft.xml")]
    (println (:options data))
    (generate-saft {:account-id (get-in data [:options :account-id])
                      :year year
                      :begin (str year "-01-01")
                      :end (str year "-12-01")
                      :output output
                      :formatted (boolean (get-in data [:options :formatted]))})))

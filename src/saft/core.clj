(ns saft.core
  (:gen-class)
  (:require [clojure.data.xml :as xml]
            [clojure.java.jdbc :as j]
            [clojure.tools.cli :as cli]
            [saft.tax-table :as tax-table]
            [saft.common :as common]
            [saft.document :as document]
            [saft.item :as item]
            [saft.client :as client]
            [saft.account :as account]
            [saft.header :as header]
            [saft.product :as product]
            [saft.accounting-relevant-totals :as accounting-relevant-totals]))

(def db {:classname "com.mysql.jdbc.Driver" 
         :subprotocol "mysql"
         :subname "//localhost:3306/invoicexpress"
         :user "root"})

(defn- fetch-all-data
  "Gets all needed data from storage"
  [{:keys [account-id begin end] :as args}]
  (let [account (account/account-query args)
        args (assoc args :account account)]

    {:account account
     :clients (client/clients-query args)
     :products (product/products-query args)
     :documents (document/documents-query args)}))

(defn preload-docs [data docs]
  (let [doc-ids (map :id docs)
        items (item/items-query data doc-ids)]
    (group-by :invoice_id items)))

(defn preload-account-versions [data docs]
  (let [account-versions (map :account_version docs)
        versions (account/account-versions-query data account-versions)]
    (group-by :version versions)))

(defn- write-documents [data docs]
  (let [cache {}
        cache {:items (preload-docs data docs)
               :account-versions (preload-account-versions data docs)}
        totals (accounting-relevant-totals/run db data)]
    (println "Totals: " totals)
    (xml/element :SalesInvoices {}
                 (xml/element :NumberOfEntries {} (:number_of_entries totals))
                 (xml/element :TotalDebit {} (:total_debit totals))
                 (xml/element :TotalCredit {} (:total_credit totals))
                 (map #(document/document-xml cache (:account data) %) docs))))

(defn write-tax-table [data]
  (let [tax-table (tax-table/run db data)]
    (xml/element :TaxTable {}
                 (map tax-table/tax-table-entry-xml tax-table))))

(defn- write-saft [data account]
  (xml/element :AuditFile {:xmlns "urn:OECD:StandardAuditFile-Tax:PT_1.03_01"
                           "xmlns:xsi" "http://www.w3.org/2001/XMLSchema-instance"}
               (header/header-xml {} account)
               (xml/element :MasterFiles {}
                            (client/clients-xml (:clients data))
                            (product/products-xml (:products data))
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

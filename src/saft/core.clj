(ns saft.core
  (:gen-class)
  (:require [clojure.data.xml :as xml]
            [clojure.java.jdbc :as j]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clj-yaml.core :as yaml]
            [clj-time.core :as t]
            [saft.tax-table :as tax-table]
            [saft.common :as common]
            [saft.document :as document]
            [saft.payment :as payment]
            [saft.item :as item]
            [saft.client :as client]
            [saft.payment-method :as payment-method]
            [saft.account :as account]
            [saft.header :as header]
            [saft.product :as product]
            [saft.payment-totals :as payment-totals]
            [saft.accounting-relevant-totals :as accounting-relevant-totals]))

(def local-db {:host "localhost"
               :dbname "invoicexpress"
               :dbtype "mysql"
               :user "root"})

(defn- fetch-all-data
  "Gets all needed data from storage"
  [{:keys [account-id begin end] :as args}]
  (let [account (account/account-query args)
        args (assoc args :account account)]
    {:account account
     :clients (client/clients-query args)
     :products (product/products-query args)}))

(defn preload-docs [data docs]
  (let [doc-ids (map :id docs)
        items (item/items-query data doc-ids)]
    (group-by :invoice_id items)))

(defn preload-payment-methods [data docs]
  (let [doc-ids (map :id docs)
        items (payment-method/payment-methods-query data doc-ids)]
    (group-by :receipt_id items)))

(defn preload-account-versions [data docs]
  (let [account-versions (map :account_version docs)
        versions (account/account-versions-query data account-versions)]
    (group-by :version versions)))

(defn- write-documents [data]
  (let [docs (document/documents-query data)
        owner-documents (document/owner-documents-query data docs)
        cache {:items (preload-docs data docs)
               :owner-documents (group-by :id owner-documents)
               :clients (group-by (fn [client]
                                    [(:client_id client) (:version client)])
                                  (:clients data))
               :account-versions (preload-account-versions data docs)}
        totals (accounting-relevant-totals/run (:db data) data)]
    (println "[INFO] Accounting relevant totals" totals)
    (xml/element :SalesInvoices {}
                 (accounting-relevant-totals/totals-xml totals)
                 (map #(document/document-xml cache (:account data) %) docs))))

(defn- write-payments [data]
  (let [totals (payment-totals/run (:db data) data)
        receipts (payment/receipts-query data)
        cache {:payment-methods (preload-payment-methods data receipts)
               :account-versions (preload-account-versions data receipts)}]
    (println "[INFO] Payment totals" totals)
    (xml/element :Payments {}
                 (payment-totals/totals-xml totals)
                 (map #(payment/payment-xml cache (:account data) %) receipts))))

(defn write-tax-table [data]
  (let [tax-table (tax-table/run (:db data) data)]
    (xml/element :TaxTable {}
                 (map tax-table/tax-table-entry-xml tax-table))))

(defn- write-saft [data account]
  (xml/element :AuditFile {:xmlns "urn:OECD:StandardAuditFile-Tax:PT_1.03_01"
                           "xmlns:xsi" "http://www.w3.org/2001/XMLSchema-instance"}
               (header/header-xml {:year (common/fiscal-year (:start-date data))
                                   :start-date (:start-date data)
                                   :end-date (:end-date data)
                                   :created (common/generated-date)} account)
               (xml/element :MasterFiles {}
                            (client/clients-xml (:clients data))
                            (product/products-xml (:products data))
                            (write-tax-table data))
               (xml/element :SourceDocuments {}
                            (write-documents data)
                            (write-payments data))))

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

(defn used-mem []
  (int (/ (- (-> (java.lang.Runtime/getRuntime) (.totalMemory)) (-> (java.lang.Runtime/getRuntime) (.freeMemory))) (* 1024 1024))))

(defn file-size [output]
  (float  (/ (.length (io/file output)) (* 1024 1024))))

(defn generate-saft
  [{:keys [account-id output formatted db begin end] :as args}]
  (common/time-info (str "[ALL] Complete SAFT [" output "]")
     (do
       (println "---------------------")
       (println "SAF-T for Account-id:" account-id "from" begin "to" end)
       (let [args (if (nil? db)
                    (assoc args :db local-db)
                    args)
             args (update-in args [:db] assoc "useTimezone" "true"
                                              "useLegacyDatetimeCode" "false"
                                              "serverTimezone" "UTC")
             args (assoc args :start-date (:begin args)
                              :end-date (:end args)
                              :begin (str (:begin args) " 00:00:00")
                              :end (str (:end args) " 23:59:59 "))
             data (merge args (fetch-all-data args))
             account (:account data)
             tags (common/time-info "[XML] Build XML structure" (write-saft data account))]
         (with-open [out-file (java.io.OutputStreamWriter. (java.io.FileOutputStream. output) "UTF-8")]
           (common/time-info "[FILE] Write to file" (xml/emit tags out-file :encoding "UTF-8")))
         (when formatted
           (let [raw (slurp output)
                 formatted (common/time-info "[FILE] Format XML" (ppxml raw))]
             (spit output formatted)))
         (println "[MEM] Used RAM:" (used-mem) "Mb")
         (println "[FILE] File size:" (file-size output) "Mb")))))

(def cli-options
  [["-a" "--account-id ID" "Account ID"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   ["-o" "--output FILE" "Output file"]

   ["-f" "--formatted" "Format XML"]

   ["-d" "--database YAML" "Database yaml file"]

   ["-e" "--env YAML" "Database yaml file env to use"]

   ["-y" "--year YEAR" "Year"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 2000 % 2020) "Must be a number between 2000 and 2020"]]

   ["-m" "--month MONTH" "Month"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 13) "Must be a number between 1 and 12"]]

   ["-h" "--help"]])

(defn load-db-conn
  [options]
  (let [yaml-file (:database options)]
    (if (nil? yaml-file)
      local-db
      (let [data (yaml/parse-string (slurp yaml-file))
            env (keyword (get options :env "development"))
            config (get data env)]
        (prn "options" options)
        (prn "data" data)
        (prn "env" env)
        (prn "config" config)
        (assoc {} :user (:username config)
                  :password (when (:password config) (:password config))
                  :host (get config :host "localhost-waza")
                  :dbname (:database config)
                  :dbtype "mysql"
                  ;:subname (str "jdbc:mysql://" (get config :host "localhost") "/" (:database config) "?user="(:username config)"&password="(:password config)"&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8")
                  )
        ))))

(defn -main [& args]
  (let [data (cli/parse-opts args cli-options)
        year (get-in data [:options :year] 2016)
        month-start (get-in data [:options :month] 1)
        month-end (if (get-in data [:options :month])
                    month-start
                    12)
        output (get-in data [:options :output] "saft.xml")
        db (load-db-conn (:options data))]
    (println "Using DB data: " db)
    (generate-saft {:account-id (get-in data [:options :account-id])
                      :year year
                      :begin (str year "-" month-start "-01")
                      :end (str year "-" month-end "-" (t/day (t/last-day-of-the-month year month-end)))
                      :output output
                      :db db
                      :formatted (boolean (get-in data [:options :formatted]))})))

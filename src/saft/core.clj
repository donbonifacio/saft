(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.core
  "Generares a SAF-T file"
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
            [saft.guide :as guide]
            [saft.payment :as payment]
            [saft.item :as item]
            [saft.client :as client]
            [saft.payment-method :as payment-method]
            [saft.payment-item :as payment-item]
            [saft.guide-item :as guide-item]
            [saft.account :as account]
            [saft.header :as header]
            [saft.product :as product]
            [saft.payment-totals :as payment-totals]
            [saft.guide-totals :as guide-totals]
            [saft.accounting-relevant-totals :as accounting-relevant-totals]))

(def local-db
  "This is the default local DB"
  {:host "localhost"
   :dbname "invoicexpress"
   :dbtype "mysql"
   :user "root"})

(defn clients-by-version
  "Given a collection of client versions, returns a collection of those clients
  grouped by the id and the version. This is useful because usually we only
  have the client_id and the version."
  [clients]
  (group-by (fn [client]
              [(:client_id client) (:version client)])
            clients))

(defn- fetch-all-data
  "Gets all needed data from storage. This acts like a cache and can have
  more or less information based on given configuration."
  [{:keys [account-id begin end] :as args}]
  (let [account (account/account-query args)
        args (assoc args :account account)]
    (assert account (str "No account for " account-id))
    (if (:preload-all-documents? args)
      (let [documents (document/documents-query args)
            receipts (payment/receipts-query args)
            guides (guide/guides-query args)
            all-docs (concat documents receipts guides)
            clients (client/clients-query-by-documents args all-docs)]
        {:account account
         :documents documents
         :receipts receipts
         :guides guides
         :clients clients
         :clients-by-version (clients-by-version clients)})
      (let [clients (client/clients-query args)]
        {:account account
         :clients clients
         :clients-by-version (clients-by-version clients)}))))

(defn- preload-doc-items
  "Fetches the invoice items for the given documents. Groups by invoice_id."
  [data docs]
  (let [doc-ids (map :id docs)
        items (item/items-query data doc-ids)]
    (group-by :invoice_id items)))

(defn- preload-guide-items
  "Fetches the invoice items for the given guides Groups by invoice_id."
  [data docs]
  (let [doc-ids (map :id docs)
        items (guide-item/guide-items-query data doc-ids)]
    (group-by :invoice_id items)))

(defn- preload-payment-methods
  "Fetches the payment methods for the given documents. Groups by receipt_id."
  [data docs]
  (let [doc-ids (map :id docs)
        items (payment-method/payment-methods-query data doc-ids)]
    (group-by :receipt_id items)))

(defn- preload-payment-items
  "Preloads the payment items for the given documents."
  [data docs]
  (let [doc-ids (map :id docs)
        items (payment-item/payment-items-query data doc-ids)]
    items))

(defn- preload-paid-documents
  "Fetches all documents for the provided ids."
  [data doc-ids]
  (let [documents (document/documents-by-ids-query data doc-ids)]
    documents))

(defn- preload-account-versions
  "Fetches all the account versions for the given documents."
  [data docs]
  (let [account-versions (map :account_version docs)
        versions (account/account-versions-query data account-versions)]
    (group-by :version versions)))

(defn- write-clients
  "Load and write the clients as SAF-T XML."
  [data]
  (let [clients (or (:clients data)
                    (client/clients-query data))]
    (client/clients-xml clients)))

(defn- write-products
  "Load and write the products as SAF-T XML."
  [data]
  (let [products (or (:products data)
                     (product/products-query data))]
    (product/products-xml products)))

(defn- write-documents
  "Load and write the documents as SAF-T XML."
  [data]
  (let [docs (or (:documents data)
                 (document/documents-query data))
        owner-documents (document/owner-documents-query data docs)
        cache {:items (preload-doc-items data docs)
               :owner-documents (group-by :id owner-documents)
               :clients (:clients-by-version data)
               :account-versions (preload-account-versions data docs)}
        totals (accounting-relevant-totals/run (:db data) data)]
    (println "[INFO] Accounting relevant totals" totals)
    (xml/element :SalesInvoices {}
                 (accounting-relevant-totals/totals-xml totals)
                 (map #(document/document-xml cache (:account data) %) docs))))

(defn- write-guides
  "Load and write the guides as SAF-T XML."
  [data]
  (let [totals (guide-totals/run (:db data) data)]
    (println "[INFO] Guide totals" totals)
    (when (not (zero? (:line_count totals)))
      (let[guides (or (:guides data)
                      (guide/guides-query data))
           guide-items (preload-guide-items data guides)
           cache {:account (:account data)
                  :items guide-items
                  :clients (:clients-by-version data)
                  :account-versions (preload-account-versions data guides)}]
        (xml/element :MovementOfGoods {}
                     (guide-totals/totals-xml totals)
                     (map #(guide/guide-xml cache (:account data) %) guides))))))

(defn- write-payments
  "Load and write the payments as SAF-T XML."
  [data]
  (let [totals (payment-totals/run (:db data) data)]
    (println "[INFO] Payment totals" totals)
    (when (not (zero? (:number_of_entries totals)))
      (let [receipts (or (:receipts data)
                         (payment/receipts-query data))
            owner-documents (document/owner-documents-query data receipts)
            payment-items (preload-payment-items data receipts)
            paid-documents (preload-paid-documents data (map :document_id payment-items))
            cache {:account (:account data)
                   :payment-methods (preload-payment-methods data receipts)
                   :payment-items (group-by :receipt_id payment-items)
                   :paid-documents (group-by :id paid-documents)
                   :owner-documents (group-by :id owner-documents)
                   :account-versions (preload-account-versions data (concat receipts paid-documents))}]
        (xml/element :Payments {}
                     (payment-totals/totals-xml totals)
                     (map #(payment/payment-xml cache (:account data) %) receipts))))))

(defn write-tax-table
  "Loads and writes the tax table."
  [data]
  (let [tax-table (tax-table/run (:db data) data)]
    (xml/element :TaxTable {}
                 (map tax-table/tax-table-entry-xml tax-table))))

(defn- write-saft
  "Loads all required information and builds the SAF-T XML."
  [data account]
  (common/time-info "[XML] Build XML structure"
    (xml/element :AuditFile {:xmlns "urn:OECD:StandardAuditFile-Tax:PT_1.03_01"
                             "xmlns:xsi" "http://www.w3.org/2001/XMLSchema-instance"}
                 (header/header-xml {:year (common/fiscal-year (:start-date data))
                                     :start-date (:start-date data)
                                     :end-date (:end-date data)
                                     :created (common/generated-date)} account)
                 (xml/element :MasterFiles {}
                              (write-clients data)
                              (write-products data)
                              (write-tax-table data))
                 (xml/element :SourceDocuments {}
                              (write-documents data)
                              (write-guides data)
                              (write-payments data)))))

(defn ppxml
  "Formats xml. It's configured to be the same formatting as IX."
  [xml]
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

(defn used-mem
  "Returns the number of MB of used memory."
  []
  (int (/ (- (-> (java.lang.Runtime/getRuntime) (.totalMemory)) (-> (java.lang.Runtime/getRuntime) (.freeMemory))) (* 1024 1024))))

(defn file-size
  "Given a file name, return its file size em MB."
  [output]
  (float  (/ (.length (io/file output)) (* 1024 1024))))

(defn generate-saft
  "Generates a saft given required parameters."
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
             tags (write-saft data account)]
         (with-open [out-file (java.io.OutputStreamWriter. (java.io.FileOutputStream. output) "UTF-8")]
           (common/time-info "[FILE] Write to file" (xml/emit tags out-file :encoding "UTF-8")))
         (when formatted
           (let [raw (slurp output)
                 formatted (common/time-info "[FILE] Format XML" (ppxml raw))]
             (spit output formatted)))
         (println "[MEM] Used RAM:" (used-mem) "Mb")
         (println "[FILE] File size:" (file-size output) "Mb")))))

(def cli-options
  "Options available to use on the CLI."
  [["-a" "--account-id ID" "Account ID"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   ["-o" "--output FILE" "Output file"]

   ["-f" "--formatted" "Format XML"]

   ["-p" "--preload-all-documents" "Preload all documents"]

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
  "Loads the db connection from a YAML file."
  [options]
  (let [yaml-file (:database options)]
    (if (nil? yaml-file)
      local-db
      (let [data (yaml/parse-string (slurp yaml-file))
            env (keyword (get options :env "development"))
            config (get data env)]
        (assoc {} :user (:username config)
                  :password (when (:password config) (:password config))
                  :host (get config :host "localhost")
                  :dbname (:database config)
                  :dbtype "mysql")))))

(defn -main
  "To be used as a CLI interface. Understands the params and configures
  the saft for them."
  [& args]
  (let [data (cli/parse-opts args cli-options)
        year (get-in data [:options :year] 2016)
        month-start (get-in data [:options :month] 1)
        month-end (if (get-in data [:options :month]) month-start 12)
        output (get-in data [:options :output] "saft.xml")
        db (load-db-conn (:options data))]
    (println "Using DB data: " db)
    (generate-saft {:account-id (get-in data [:options :account-id])
                    :year year
                    :begin (str year "-" month-start "-01")
                    :end (str year "-" month-end "-" (t/day (t/last-day-of-the-month year month-end)))
                    :output output
                    :preload-all-documents? (boolean (get-in data [:options :preload-all-documents]))
                    :db db
                    :formatted (boolean (get-in data [:options :formatted]))})))

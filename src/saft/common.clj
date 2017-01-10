(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.common
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [clj-time.format :as f]
    [clj-time.coerce :as c]
    [clj-time.core :as t]))

(def unknown
  "This represents the unknown string, in Portuguese."
  "Desconhecido")

(defn get-str
  "Gets and prepares a string value from a map"
  ([m k]
    (get-str m k 10000))
  ([m k size]
   (get-str m k size unknown))
  ([m k size default-value]
   (let [value (get m k)]
     (if (or (nil? value) (empty? value))
       default-value
       (apply str (take size (filter #(<= 32 (int %)) value)))))))

(defmacro time-info
  "This is a copy of the time macro of std clojure. It adds an addition
  string label and outputs it."
  [info expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println (str ~info ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))

(defmacro query-time-info
  "This is a copy of the time macro of std clojure. It adds an addition
  string label and outputs it. It assumes that the result of expr will be
  a collection and outputs it's size."
  [info expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println (str ~info ": "
                   (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs "
                   "(loaded " (if (map? ret#) 1 (count ret#)) " rows)"))
     ret#))

(defn do-query
  "Performs the given query against the db. Tracks the time and if with
  error, outputs the full query."
  [db label sql]
  (query-time-info label
    (try
      (j/query db sql)
      (catch Exception e
        (println "Error running SQL query:" label sql)
        (throw e)))))

(defn debit-documents
  "All the possible debit documents."
  ([]
   (debit-documents nil))
  ([account]
   ["Invoice" "CashInvoice" "DebitNote" "InvoiceReceipt" "SimplifiedInvoice"]))

(defn credit-documents
  "All the possible credit documents."
  ([]
   (credit-documents nil))
  ([account]
   ["CreditNote"]))

(defn guide-documents
  "All the possible guide documents."
  ([]
   (guide-documents nil))
  ([account]
   ["Shipping" "Devolution" "Transport"]))

(defn guide?
  "Given a type name, returns true if the type is a Guide."
  [type-name]
  (boolean (first (filter #(= type-name %) (guide-documents)))))

(defn payment-documents
  "All the possible payment documents."
  ([]
   (payment-documents nil))
  ([account]
   ["Receipt"]))

(defn statuses-relevant-for-communication
  "The list of statuses that should be considered for communication."
  []
  ["sent" "settled" "second_copy" "canceled"])

(defn invoice?
  "True if the document type is an invoice."
  [doc-type]
  (= "Invoice" doc-type))

(defn db-string-coll
  "Given a coll of strings, returns each element surrounded by '.
  Removes Invoice, because this is to be used on queries and Invoice
  maps to null."
  [coll]
  (->> coll
       (remove invoice?)
       (map #(str "'" %  "'"))))

(defn saft-status-str
  "Returns the saft status as a db usable string."
  []
  (clojure.string/join ","
    (db-string-coll (statuses-relevant-for-communication))))

(defn types-condition
  "Given a coll of type names, returns the query part for them."
  [types]
  (let [db-types (db-string-coll types)]
    (str "(" (when (first (filter invoice? types))
                "invoices.type is null or ")
         "invoices.type in (" (clojure.string/join "," db-types) "))")))

(defn saft-types-condition
  "The condition for the types that make sense on saft."
  [account]
  (types-condition
     (concat (debit-documents account)
             (credit-documents account))))

(defn saft-guides-condition
  "The condition for the guide types for saft."
  [account]
  (types-condition
     (guide-documents account)))

(defn all-types-condition
  "A condition for all types that could be on a saft."
  [account]
  (types-condition
     (concat (debit-documents account)
             (credit-documents account)
             (payment-documents account)
             (guide-documents account))))

(def date-formatter
  "This is the simple Date format for saft."
  (f/formatter "yyyy-MM-dd"))

(def saft-formatter
  "This is the simple DateTime format for saft"
  (f/formatters :date-hour-minute-second))

(defn generated-date 
  "Outputs the generated date string for saft."
  ([]
   (generated-date (t/now)))
  ([datetime]
   (f/unparse date-formatter datetime)))

(defn fiscal-year
  "Gets the year of a date."
  [date]
  (t/year (f/parse date-formatter date)))

(defn saft-date
  "Gets a proper date string from the arg. Arg can be string or an sql date"
  [date]
  (if (string? date)
    date
    (f/unparse saft-formatter (c/from-sql-date date))))

(defn month
  "Gets the month of a date."
  [date]
  (cond
    (string? date) (t/month (f/parse date))
    :else (t/month (c/from-sql-date date))))

(defn get-date
  "Given a key and a map, fetches the value and returns it as a SAFT date."
  [m k]
  (let [date (get m k)]
    (cond
      (string? date) (f/unparse date-formatter (f/parse date))
      :else (f/unparse date-formatter (c/from-sql-date date)))))

(defn get-date-time
  "Given a key and a map, fetches the value and returns it as a SAFT datetime."
  [m k]
  (let [date (get m k)]
    (cond
      (string? date) (f/unparse saft-formatter (f/parse date))
      :else (f/unparse saft-formatter (c/from-sql-date date)))))


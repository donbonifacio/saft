(ns saft.common
  (:require
    [clojure.data.xml :as xml]
    [clj-time.format :as f]
    [clj-time.coerce :as c]
    [clj-time.core :as t]))

(def unknown "Desconhecido")

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
  [info expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println (str ~info ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))

(defn debit-documents [account]
  ["Invoice" "CashInvoice" "DebitNote" "InvoiceReceipt" "SimplifiedInvoice"])

(defn credit-documents [account]
  ["CreditNote"])

(defn guide-documents [account]
  ["Shipping" "Devolution" "Transport"])

(defn guide? [type-name]
  (first (filter #(= type-name %) guide-documents)))

(defn payment-documents [account]
  ["Receipt"])

(defn statuses-relevant-for-communication []
  ["sent" "settled" "second_copy" "canceled"])

(defn invoice? [doc-type]
  (= "Invoice" doc-type))

(defn db-string-coll [coll]
  (->> coll
       (remove invoice?)
       (map #(str "'" %  "'"))))

(defn saft-status-str
  []
  (clojure.string/join ","
    (db-string-coll (statuses-relevant-for-communication))))

(defn types-condition [types]
  (let [db-types (db-string-coll types)]
    (str "(" (when (first (filter invoice? types))
                "invoices.type is null or ")
         "invoices.type in (" (clojure.string/join "," db-types) "))")))

(defn saft-types-condition [account]
  (types-condition
     (concat (debit-documents account)
             (credit-documents account))))

(defn all-types-condition [account]
  (types-condition
     (concat (debit-documents account)
             (credit-documents account)
             (payment-documents account)
             (guide-documents account))))

(def date-formatter (f/formatter "yyyy-MM-dd"))
(def saft-formatter (f/formatters :date-hour-minute-second))

(defn generated-date []
  (f/unparse date-formatter (t/now)))

(defn fiscal-year [date]
  (t/year (f/parse date-formatter date)))

(defn saft-date [date]
  (if (string? date)
    date
    (f/unparse saft-formatter (c/from-sql-date date))))

(defn month [date]
  (cond
    (string? date) (t/month (f/parse date))
    :else (t/month (c/from-sql-date date))))

(defn get-date [m k]
  (let [date (get m k)]
    (cond
      (string? date) (f/unparse date-formatter (f/parse date))
      :else (f/unparse date-formatter (c/from-sql-date date)))))

(defn get-date-time [m k]
  (let [date (get m k)]
    (cond
      (string? date) (f/unparse saft-formatter (f/parse date))
      :else (f/unparse saft-formatter (c/from-sql-date date)))))


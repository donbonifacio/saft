(ns saft.common
  (:require
    [clojure.data.xml :as xml]))

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

(defn get-date [m k]
  (str (get m k)))

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

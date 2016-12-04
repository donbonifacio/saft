(ns saft.common
  (:require
    [clojure.data.xml :as xml]))

(defn get-str [m k]
  (apply str (filter #(<= 32 (int %) 126) (get m k ""))))

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


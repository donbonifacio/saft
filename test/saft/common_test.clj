(ns saft.common-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [saft.common :as common]))

(deftest get-str-test
  (is (= "Hello" (common/get-str {:k "Hello"} :k)))
  (is (= "He" (common/get-str {:k "Hello"} :k 2)))
  (is (= "Hello" (common/get-str {} :k 2 "Hello")))
  (is (= "Hello" (common/get-str {:k ""} :k 2 "Hello")))
  (is (= common/unknown (common/get-str {} :k 2))))

(deftest guide?-test
  (is (true? (common/guide? "Shipping")))
  (is (true? (common/guide? "Transport")))
  (is (true? (common/guide? "Devolution")))
  (is (false? (common/guide? "Invoice")))
  (is (false? (common/guide? "CreditNote")))
  (doseq [guide-name (common/guide-documents)]
    (is (true? (common/guide? guide-name))))
  (doseq [doc-name (concat (common/debit-documents)
                           (common/credit-documents)
                           (common/payment-documents))]
    (is (false? (common/guide? doc-name)))))

(deftest types-condition-test
  (is (= "(invoices.type is null or invoices.type in ('InvoiceReceipt'))"
         (common/types-condition ["Invoice" "InvoiceReceipt"])))
  (is (= "(invoices.type in ('CreditNote'))"
         (common/types-condition ["CreditNote"])))
  (is (= "(invoices.type in ('DebitNote','SimplifiedInvoice'))"
         (common/types-condition ["DebitNote" "SimplifiedInvoice"]))))

(deftest db-string-coll-test
  (is (= ["'CreditNote'"] (common/db-string-coll ["CreditNote" "Invoice"])))
  (is (= ["'CreditNote'"] (common/db-string-coll ["CreditNote"])))
  (is (= ["'CreditNote'", "'DebitNote'"] (common/db-string-coll ["CreditNote" "DebitNote"]))))

(deftest saft-status-str-test
  (is (= "'sent','settled','second_copy','canceled'"
         (common/saft-status-str))))

(deftest types-condition-test
  (is (= "(invoices.type is null or invoices.type in ('CreditNote','DebitNote'))"
         (common/types-condition ["Invoice"  "CreditNote" "DebitNote"])))
  (is (= "(invoices.type in ('CreditNote','DebitNote'))"
         (common/types-condition ["CreditNote" "DebitNote"]))))

(deftest saft-types-condition-test
  (is (= "(invoices.type is null or invoices.type in ('CashInvoice','DebitNote','InvoiceReceipt','SimplifiedInvoice','CreditNote'))"
         (common/saft-types-condition {}))))

(deftest saft-guides-condition-test
  (is (= "(invoices.type in ('Shipping','Devolution','Transport'))"
         (common/saft-guides-condition {}))))

(deftest all-types-condition-test
  (is (= "(invoices.type is null or invoices.type in ('CashInvoice','DebitNote','InvoiceReceipt','SimplifiedInvoice','CreditNote','Receipt','Shipping','Devolution','Transport'))"
         (common/all-types-condition {}))))

(deftest generated-date-test
  (is (= "2017-01-01" (common/generated-date (t/date-time 2017 1 1)))))

(deftest fiscal-year-test
  (is (= 2017 (common/fiscal-year "2017-01-01"))))

(deftest month-test
  (is (= 1 (common/month "2017-01-01"))))

(deftest get-date-test
  (is (= "2017-01-01"
         (common/get-date {:date "2017-01-01"} :date))))

(deftest get-date-time-test
  (is (= "2017-01-01T00:00:00"
         (common/get-date-time {:date "2017-01-01"} :date))))

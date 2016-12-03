(ns saft.accounting-relevant-totals-test
  (:require [clojure.test :refer :all]
            [saft.accounting-relevant-totals :as accounting-relevant-totals]))

(deftest types-condition-test
  (is (= "(invoices.type is null or invoices.type in ('InvoiceReceipt'))"
         (accounting-relevant-totals/types-condition ["Invoice" "InvoiceReceipt"])))
  (is (= "(invoices.type in ('CreditNote'))"
         (accounting-relevant-totals/types-condition ["CreditNote"])))
  (is (= "(invoices.type in ('DebitNote','SimplifiedInvoice'))"
         (accounting-relevant-totals/types-condition ["DebitNote" "SimplifiedInvoice"]))))

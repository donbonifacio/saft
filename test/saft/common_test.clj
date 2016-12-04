(ns saft.common-test
  (:require [clojure.test :refer :all]
            [saft.common :as common]))

(deftest types-condition-test
  (is (= "(invoices.type is null or invoices.type in ('InvoiceReceipt'))"
         (common/types-condition ["Invoice" "InvoiceReceipt"])))
  (is (= "(invoices.type in ('CreditNote'))"
         (common/types-condition ["CreditNote"])))
  (is (= "(invoices.type in ('DebitNote','SimplifiedInvoice'))"
         (common/types-condition ["DebitNote" "SimplifiedInvoice"]))))

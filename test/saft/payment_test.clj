(ns saft.payment-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.payment :as payment]))

(deftest payment-xml-test
  (let [account
          {:id 5554}

        invoice
          {:total_taxes 10}

        payment-methods
          [{:payment_mechanism "MB"
            :amount 10
            :payment_date "12-12-2019"}]

        payment-items
          [{:tax 1}]

        cache
          {:owner-documents {1 [invoice]}
           :payment-methods {1 payment-methods}
           :paid-documents {1 [invoice]}
           :payment-items {1 payment-items}}

        receipt
          {:id 206417
           :type "Receipt"
           :owner_invoice_id 1
           :date "2016-01-01"
           :created_at "2016-07-01T15:06:33"
           :final_date "2016-07-01T15:06:33"
           :saft_hash "N9rCtcNw7IPkCZQa7rOS28nxcb1AerYOJI8cJZaGuxPwgzWHCzIAsF8B2C5VK5tso6Bqe+pu0ixTgYgehxAwLeK9s9tT4IJMDBlodAwi9lzCdvq2GKU3NwT7aId+3ODyKBYoERAu+wxWAN7Qq+W9cOC7K4FeTbYLgWN2PqP9NIs="
           :sequence_number "FT 2013/69"
           :document_serie "2013"
           :document_number "69"
           :status "sent"
           :tax 3.68
           :total 19.68M
           :total_taxes 3.68M
           :total_with_taxes 19.68M
           :total_before_taxes 16.00M}

        expected
          (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
               "<Payment>"
                  "<PaymentRefNo>RG 2013/69</PaymentRefNo>"
                  "<TransactionDate>2016-01-01</TransactionDate>"
                  "<PaymentType>RG</PaymentType>"
                  "<SystemID>206417</SystemID>"
                  "<DocumentStatus>"
                    "<PaymentStatus>N</PaymentStatus>"
                    "<PaymentStatusDate>2016-07-01T15:06:33</PaymentStatusDate>"
                    "<SourceID>5554</SourceID>"
                    "<SourcePayment>P</SourcePayment>"
                  "</DocumentStatus>"
                  "<SourceID>5554</SourceID>"
                  "<SystemEntryDate>2016-07-01T15:06:33</SystemEntryDate>"
                  "<CustomerID>0</CustomerID>"
                  "<DocumentTotals>"
                    "<TaxPayable>3.68</TaxPayable>"
                    "<NetTotal>16.00</NetTotal>"
                    "<GrossTotal>26.00</GrossTotal>"
                  "</DocumentTotals>"
                "</Payment>")]
    (is (= expected
           (xml/emit-str (payment/payment-xml cache account receipt))))))

(deftest payment-type-test
  (is (= "RC" (payment/payment-type {:iva_caixa true})))
  (is (= "RG" (payment/payment-type {:iva_caixa false})))
  (is (= "RG" (payment/payment-type {}))))

(deftest receipt-number-test
  (is (= "RG A/1" (payment/receipt-number {} {:document_serie "A" :document_number "1"})))
  (is (= "RC A/1" (payment/receipt-number {:iva_caixa true} {:document_serie "A" :document_number "1"}))))

(deftest gross-total-test
  (is (= 20 (payment/gross-total {:total_taxes 10} {:total_before_taxes 10}))))

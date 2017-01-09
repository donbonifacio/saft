(ns saft.item-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.common :as common]
            [saft.item :as item]))

(deftest description-test
  (is (= common/unknown (item/description {:name ""})))
  (is (= "Name" (item/description {:name "Name"})))
  (is (= "Description" (item/description {:description "Description" :name "Name"}))))

(deftest exempt?-test
  (is (true? (item/exempt? {:tax_value 0})))
  (is (false? (item/exempt? {:tax_value 10})))
  (is (false? (item/exempt? {}))))

(deftest tax-exemption-reason-test
  (is (= "Test" (item/tax-exemption-reason {:tax_exemption_message "Test"})))
  (is (= "Não sujeito; não tributado (ou similar)." (item/tax-exemption-reason {}))))

(deftest discount-applied?-test
  (is (true? (item/discount-applied? {:discount_amount 10})))
  (is (false? (item/discount-applied? {:discount_amount 0})))
  (is (false? (item/discount-applied? {}))))

(deftest settlement-amount-test
  (is (= 10 (item/settlement-amount {:discount_amount 10}))))

(deftest tax-region-from-item-test
  (is (= "PT" (item/tax-region-from-item {})))
  (is (= "Açores" (item/tax-region-from-item {:tax_region "Açores"}))))

(deftest tax-region-test
  (is (= "PT" (item/tax-region {:country "Portugal"} {}))))

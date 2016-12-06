(ns saft.countries-test
  (:require [clojure.test :refer :all]
            [saft.countries :as countries]))

(deftest countries-code
  (is (= "DE" (countries/country-code "Germany"))
  (is (= "PT" (countries/country-code "Portugal")))))

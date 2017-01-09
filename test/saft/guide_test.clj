(ns saft.guide-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.guide :as guide]))

(deftest guide-xml-test
  (let [account
          {:id 5554}

        guide
          {:id 206417
           :type "Shipping"
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
           :total_before_taxes 16.00M

           :address_to_detail "Address to detail"
           :address_to_city "Address to city"
           :address_to_postal_code "1234-123"
           :address_to_country "Portugal"

           :address_from_detail "Address from detail"
           :address_from_city "Address from city"
           :address_from_postal_code "1234-123"
           :address_from_country "Portugal"

           :loaded_at "2016-07-01T15:06:33"

           :items [{:name "Item 8"
                    :product_id 8
                    :description "D5c194mpfz8h"
                    :quantity "2.0"
                    :unit_price "8.0"
                    :unit "unit"
                    :subtotal "16.0"
                    :tax_value 23.0
                    :tax_point_date "2016-01-01"}]}

        expected
          (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                "<StockMovement>"
                  "<DocumentNumber>GR 2013/69</DocumentNumber>"
                  "<DocumentStatus>"
                    "<MovementStatus>N</MovementStatus>"
                    "<MovementStatusDate>2016-07-01T15:06:33</MovementStatusDate>"
                    "<SourceID>5554</SourceID>"
                    "<SourceBilling>P</SourceBilling>"
                  "</DocumentStatus>"
                  "<Hash>N9rCtcNw7IPkCZQa7rOS28nxcb1AerYOJI8cJZaGuxPwgzWHCzIAsF8B2C5VK5tso6Bqe+pu0ixTgYgehxAwLeK9s9tT4IJMDBlodAwi9lzCdvq2GKU3NwT7aId+3ODyKBYoERAu+wxWAN7Qq+W9cOC7K4FeTbYLgWN2PqP9NIs=</Hash>"
                  "<MovementDate>2016-07-01</MovementDate>"
                  "<MovementType>GR</MovementType>"
                  "<SystemEntryDate>2016-07-01T15:06:33</SystemEntryDate>"
                  "<CustomerID>0</CustomerID>"
                  "<SourceID>5554</SourceID>"
                  "<ShipTo>"
                    "<Address>"
                      "<AddressDetail>Address to detail</AddressDetail>"
                      "<City>Address to city</City>"
                      "<PostalCode>1234-123</PostalCode>"
                      "<Country>PT</Country>"
                    "</Address>"
                  "</ShipTo>"
                  "<ShipFrom>"
                    "<Address>"
                      "<AddressDetail>Address from detail</AddressDetail>"
                      "<City>Address from city</City>"
                      "<PostalCode>1234-123</PostalCode>"
                      "<Country>PT</Country>"
                    "</Address>"
                  "</ShipFrom>"
                  "<MovementStartTime>2016-07-01T15:06:33</MovementStartTime>"
                  "<Line>"
                    "<LineNumber>1</LineNumber>"
                    "<ProductCode>8</ProductCode>"
                    "<ProductDescription>Item 8</ProductDescription>"
                    "<Quantity>2.0</Quantity>"
                    "<UnitOfMeasure>unit</UnitOfMeasure>"
                    "<UnitPrice>8.0</UnitPrice>"
                    "<Description>D5c194mpfz8h</Description>"
                    "<CreditAmount>16.0</CreditAmount>"
                    "<Tax>"
                      "<TaxType>IVA</TaxType>"
                      "<TaxCountryRegion>PT</TaxCountryRegion>"
                      "<TaxCode>NOR</TaxCode>"
                      "<TaxPercentage>23.0</TaxPercentage>"
                    "</Tax>"
                  "</Line>"
                  "<DocumentTotals>"
                    "<TaxPayable>3.68</TaxPayable>"
                    "<NetTotal>16.00</NetTotal>"
                    "<GrossTotal>19.68</GrossTotal>"
                  "</DocumentTotals>"
                "</StockMovement>")]
    (is (= expected
           (xml/emit-str (guide/guide-xml {} account guide))))))


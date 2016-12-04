(ns saft.product-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.product :as product]))

(deftest product-xml-test
  (let [product
          {:id 206417
           :name "Item 3"
           :description "w2WxmKul00"}

        expected
          (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                "<Product>"
                  "<ProductType>S</ProductType>"
                  "<ProductCode>Item 3</ProductCode>"
                  "<ProductDescription>w2WxmKul00</ProductDescription>"
                  "<ProductNumberCode>Item 3</ProductNumberCode>"
                "</Product>")]
    (is (= expected
           (xml/emit-str (product/product-xml product))))))

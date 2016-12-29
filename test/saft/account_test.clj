(ns saft.account-test
  (:require [clojure.test :refer :all]
            [clojure.data.xml :as xml]
            [saft.account :as account]))

(deftest for-document-test
  (let [version-1 {:version 1}
        default-account {}
        cache {:account-versions {1 [version-1]}}]
    (is (= version-1
           (account/for-document cache default-account {:account_version 1})))
    (is (= default-account
           (account/for-document {} default-account {:account_version 1})))
    (is (= default-account
           (account/for-document cache default-account {:account_version 10})))))

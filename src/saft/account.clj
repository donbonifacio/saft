(ns saft.account
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn account-query
  [{:keys [db account-id] :as args}]
  (common/time-info "[SQL] Fetch account"
    (first (j/query db ["select * from accounts where id = ?" account-id]))))

(defn account-versions-query
  [{:keys [db account] :as data} account-versions]
  (if-let [account-versions (->> account-versions
                                 (remove nil?)
                                 (distinct)
                                 (seq))]
    (common/time-info (str "[SQL] Fetch " (count account-versions) " account versions")
       (j/query db [(str
                      "select id, version, iva_caixa, factura_recibo
                      from account_versions
                      where account_id = " (:id account) "
                      and version in (" (clojure.string/join "," account-versions) ")")]))
    []))


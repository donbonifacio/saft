(ns ^{:added "0.1.0" :author "Pedro Pereira Santos"}
  saft.account
  "All the interactions with the account model"
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn account-query
  "Fetches the account with the given id."
  [{:keys [db account-id] :as args}]
  (common/query-time-info "[SQL] Fetch account"
    (first (j/query db ["select * from accounts where id = ?" account-id]))))

(defn account-versions-query
  "Given a collection of account versions, returns all account objects
  for those versions."
  [{:keys [db account] :as data} account-versions]
  (if-let [account-versions (->> account-versions
                                 (remove nil?)
                                 (distinct)
                                 (seq))]
    (common/query-time-info (str "[SQL] Fetch " (count account-versions) " account versions")
       (j/query db [(str
                      "select id, version, iva_caixa, factura_recibo
                      from account_versions
                      where account_id = " (:id account) "
                      and version in (" (clojure.string/join "," account-versions) ")")]))
    []))

(defn for-document
  "Gets the given account or the version of available on cache"
  [cache account doc]
  (if-let [version (:account_version doc)]
    (if-let [account-version (first (get-in cache [:account-versions version] account))]
      account-version
      account)
    account))

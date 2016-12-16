(ns saft.payment-item
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.document :as document]
    [saft.common :as common]))

(defn payment-items-query
  [{:keys [db]} doc-ids]
  (if-let [doc-ids (seq doc-ids)]
    (common/time-info (str "[SQL] Fetch payment items for " (count doc-ids) " document(s)")
               (j/query db [(str "select receipt_id, tax, document_id
                                  from receipt_datas
                                  where receipt_id in ("(clojure.string/join "," doc-ids)")
                                  order by id")]))
    []))

(defn payment-item-xml [idx item cache]
  (let [paid-document (first (get-in cache [:paid-documents (:document_id item)]))
        account (:account cache)]
    (xml/element :Line {}
                 (xml/element :LineNumber {} (inc idx))
                 (xml/element :SourceDocumentID {}
                              (xml/element :OriginatingON {} (document/number cache account paid-document))
                              (xml/element :InvoiceDate {} (common/get-date paid-document :date)))
                 (if (= "CreditNote" (:type paid-document))
                   (xml/element :DebitAmount {} (* -1 (:tax item)))
                   (xml/element :CreditAmount {} (:tax item))))))

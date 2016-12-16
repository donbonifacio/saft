(ns saft.payment-method
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.jdbc :as j]
    [saft.common :as common]))

(defn payment-methods-query
  [{:keys [db]} doc-ids]
  (if-let [doc-ids (seq doc-ids)]
    (common/query-time-info (str "[SQL] Fetch payment methods for " (count doc-ids) " document(s)")
               (j/query db [(str "select amount, payment_mechanism, payment_date,
                                         receipt_id
                                  from partial_payments
                                  where receipt_id in ("(clojure.string/join "," doc-ids)")
                                  order by payment_date")]))
    []))

(defn payment-method-xml [item]
  (xml/element :PaymentMethod {}
               (xml/element :PaymentMechanism {} (:payment_mechanism item))
               (xml/element :PaymentAmount {} (:amount item))
               (xml/element :PaymentDate {} (common/get-date item :payment_date))))

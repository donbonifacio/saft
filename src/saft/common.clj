(ns saft.common
  (:require
    [clojure.data.xml :as xml]))

(defn get-str [m k]
  (apply str (filter #(<= 32 (int %) 126) (get m k ""))))

(defn get-date [m k]
  (str (get m k)))


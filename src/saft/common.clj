(ns saft.common
  (:require
    [clojure.data.xml :as xml]))

(defn get-str [m k]
  (apply str (filter #(<= 32 (int %) 126) (get m k ""))))

(defn get-date [m k]
  (str (get m k)))

(defmacro time-info
  [info expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println (str ~info ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))


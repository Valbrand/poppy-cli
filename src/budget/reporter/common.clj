(ns budget.reporter.common
  (:require [clojure.string :as str]))

(def ^:dynamic *indentation* 0)

(defmacro indent
  [n & body]
  `(binding [*indentation* (+ ~n *indentation*)]
     ~@body))

(defn println'
  [& stuff]
  (print (str/join (repeat *indentation* \space)))
  (apply println stuff))
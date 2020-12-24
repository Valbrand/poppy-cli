(ns budget.reporter.common
  (:require [budget.logic.money :as logic.money]
            [budget.model.money :as model.money]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def ^:dynamic *indentation* 0)

(defmacro indent
  [n & body]
  `(binding [*indentation* (+ ~n *indentation*)]
     ~@body))

(defn println'
  [& stuff]
  (print (str/join (repeat *indentation* \space)))
  (apply println stuff))

(ds/defn print-monetary-values
  [values :- (s/coll-of ::model.money/money)]
  (let [formatted-values (map logic.money/money->string values)
        max-length (count (apply max-key count formatted-values))
        format-string (str "%" max-length "s")]
    (doseq [value formatted-values]
      (println' (format format-string value)))))
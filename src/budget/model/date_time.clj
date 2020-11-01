(ns budget.v2.model.date-time
  (:require [clojure.spec.alpha :as s]
            [clj-time.format :as time.format]))

(def str->date
  (partial time.format/parse-local-date (time.format/formatter-local "yyyy/MM/dd")))

(defn date
  [x]
  (cond
    (instance? org.joda.time.LocalDate x)
    x
    
    (string? x)
    (str->date x)
    
    :else
    ::s/invalid))
(s/def ::date (s/conformer date))


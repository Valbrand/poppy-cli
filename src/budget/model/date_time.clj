(ns budget.model.date-time
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as time]
            [clj-time.local :as local]
            [clj-time.coerce :as coerce]
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

(defn now
  []
  (local/local-now))

(defn today
  []
  (coerce/to-local-date (now)))

(defn not-after?
  [a b]
  (not (time/after? a b)))

(comment
  (not-after? (str->date "2020/12/27") (today)))
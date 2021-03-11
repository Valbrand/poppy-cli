(ns budget.model.date-time
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as time]
            [clj-time.local :as local]
            [clj-time.coerce :as coerce]
            [clj-time.format :as time.format]
            [net.danielcompton.defn-spec-alpha :as ds]))

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

(ds/defn year :- int?
  [date :- ::date]
  (time/year date))

(ds/defn month-year :- ::date
  [date :- ::date]
  (let [year (time/year date)
        month (time/month date)]
    (time/local-date year month 1)))

(def ^:private month-year-formatter
  (time.format/formatter-local "yyyy/MM"))

(ds/defn month-year-string :- string?
  [date :- ::date]
  (time.format/unparse-local month-year-formatter date))

(comment
  (not-after? (str->date "2020/12/27") (today))

  (->> ((juxt time/year time/month (constantly 1)) (str->date "2020/01/17"))
       (apply time/local-date))
  (time/local-date 2021 2 1)

  (month-year-string (str->date "2020/12/27"))

  (-> (now)
      coerce/to-date-time
      #_(time/floor time/month))

  (time/month (today)))
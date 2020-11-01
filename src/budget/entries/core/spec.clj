(ns budget.v2.entries.core.spec
  (:require [budget.v2.model.date-time :as date-time]
            [budget.v2.utils :as utils]
            [clojure.spec.alpha :as s]))

(s/def ::account-name string?)
(s/def ::currency keyword?)

(s/def :meta/description string?)
(s/def :meta/tags (s/coll-of string? :kind set?))
(s/def :meta/created-at ::date-time/date)

(defn config->spec
  [config]
  (utils/apply-macro clojure.spec.alpha/or
                     (->> config
                          (map (juxt first (comp :spec second)))
                          flatten)))

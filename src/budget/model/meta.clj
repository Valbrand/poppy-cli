(ns budget.model.meta
  (:require [clojure.spec.alpha :as s]
            [clj-time.spec :as time.spec]))

(s/def :meta/created-at ::time.spec/local-date)
(s/def :meta/description string?)
(s/def :meta/single-tag string?)
(s/def :meta/tags (s/coll-of :meta/single-tag))

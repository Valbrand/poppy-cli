(ns budget.v2.validator.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::message string?)
(s/def ::details map?)
(s/def ::error (s/keys :req-un [:error/message]
                       :opt-un [:error/details]))
(s/def ::warning (s/keys :req-un [:error/message]
                         :opt-un [:error/details]))

(s/def ::validation-config-map (s/map-of keyword? fn?))

(s/def ::errors (s/coll-of ::error))
(s/def ::warnings (s/coll-of ::warning))
(s/def ::entry any?)

(s/def ::base-validation-report (s/keys :req-un [::errors ::warnings ::entry]))

(defn entry-conforms-to
  "Intended to be internal API. Public due to being exposed in a macro"
  [spec]
  (fn [val]
    (let [conformed-entry (s/conform spec (:entry val))]
      (if (s/invalid? conformed-entry)
        conformed-entry
        (assoc val :entry conformed-entry)))))

(defn unform-validation-report
  "Intended to be internal API. Public due to being exposed in a macro"
  [spec]
  (fn [val]
    (update val :entry s/unform spec)))

(defmacro validation-report-spec
  "Returns a spec that asserts that the value conforms to the :budget.v2.validator.core/base-validation-report spec and contains a key :entry which value conforms to the spec passed as the parameter.
   
   Example:
   (clojure.spec.alpha/valid? (validation-report-spec string?) {:errors [] :warnings [] :entry \"\"})"
  [spec]
  `(s/and ::base-validation-report
          (s/conformer
           (entry-conforms-to ~spec)
           (unform-validation-report ~spec))))
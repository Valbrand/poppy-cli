(ns budget.v2.model.money
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(defn money
  "Conformer for the :budget.v2.model.money/value spec"
  [x]
  (if (integer? x)
    (bigint x)
    ::s/invalid))

(s/def ::value (s/with-gen
                (s/conformer money)
                #(gen/fmap bigint (gen/int))))
(s/def ::currency (s/with-gen keyword?
                              #(gen/fmap keyword 
                                         (gen/keyword))))

(s/def ::money (s/keys :req [::value ::currency]))

(def add +)
(def zero-value? clojure.core/zero?)
(def zero? (comp zero-value? :value))

(def ZERO (s/conform ::value 0))
(ns budget.model.money
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [net.danielcompton.defn-spec-alpha :as ds]))

(defn money-conformer
  "Conformer for the :budget.model.money/value spec"
  [x]
  (if (integer? x)
    (bigint x)
    ::s/invalid))

(s/def ::value (s/with-gen
                (s/conformer money-conformer)
                #(gen/fmap bigint (gen/int))))
(s/def ::currency (s/with-gen keyword?
                              #(gen/fmap keyword 
                                         (gen/keyword))))

(s/def ::money (s/keys :req-un [::value ::currency]))

(def add +)
(def zero-value? clojure.core/zero?)
(def zero? (comp zero-value? :value))

(ds/defn money :- ::money
  [value :- ::value
   currency :- ::currency]
  {:value    value
   :currency currency})

(def ZERO (s/conform ::value 0))

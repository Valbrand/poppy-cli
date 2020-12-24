(ns budget.model.money
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [net.danielcompton.defn-spec-alpha :as ds]))

(defn money-conformer
  "Conformer for the :budget.model.money/value spec"
  [x]
  (try
    (bigdec x)
    (catch Exception _
      ::s/invalid)))

(defn- finite?
  [n]
  (Double/isFinite n))
(s/def ::value (s/with-gen
                (s/conformer money-conformer)
                #(gen/fmap bigdec
                           (gen/such-that finite? (gen/double)))))
(s/def ::currency (s/with-gen keyword?
                              #(gen/fmap keyword 
                                         (gen/keyword))))

(s/def ::money (s/keys :req-un [::value ::currency]))

(ds/defn money :- ::money
  [value :- ::value
   currency :- ::currency]
  {:value    value
   :currency currency})

(def ZERO (s/conform ::value 0))

(comment
  (clojure.test.check.generators/such-that)
  (Double/isFinite ##NaN)
  (Double/isFinite ##Inf)
  (clojure.test.check.generators/sample (gen/double))
  (clojure.test.check.generators/sample (s/gen ::money)))

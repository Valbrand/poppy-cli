(ns budget.model.account
  (:require [budget.model.meta]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def valid-account-types #{"assets"
                           "liabilities"
                           "expenses"
                           "incomes"
                           "equity"
                           "budget"
                           "goal"})
(s/def ::account-type valid-account-types)

(ds/defn valid-account-name
  [name :- string?]
  (let [[account-type & name-parts :as all-parts] (str/split name #"/")]
    (if (and (contains? valid-account-types account-type)
             (every? (complement empty?) name-parts))
      (str/join "/" all-parts)
      ::s/invalid)))
(def account-name-generator
  (gen/bind (gen/tuple (gen/elements valid-account-types)
                        (gen/vector
                         (gen/such-that (complement empty?)
                                        (gen/string-alphanumeric))
                         1 3))
             (fn [[acc-type rest]]
               (gen/return (str/join "/" (cons acc-type rest))))))
(s/def :account/name
  (s/with-gen
   (s/conformer valid-account-name)
   (constantly account-name-generator)))

(s/def :account/meta (s/keys :opt [:meta/created-at]))
(s/def ::account (s/keys :opt [:account/name :meta/created-at]))

(ds/defn new-account :- ::account
  ([name :- :account/name]
   (new-account name {}))
  ([name :- :account/name
    meta-attrs :- :account/meta]
   (let [{:meta/keys [created-at]} meta-attrs]
     #:account {:name            name
                :meta/created-at created-at})))

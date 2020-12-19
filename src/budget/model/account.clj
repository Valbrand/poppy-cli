(ns budget.model.account
  (:require [budget.model.money :as model.money]
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

(s/def :account/transactions (s/coll-of :budget.model.transaction/transaction))

(s/def ::account (s/keys :opt [:account/name :account/transactions]))

(ds/defn new-account :- ::account
  [name :- :account/name
   transactions :- :account/transactions]
  #:account {:name         name
             :transactions transactions})
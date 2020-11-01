(ns budget.v2.model.transaction
  (:require [budget.v2.model.money :as model.money]
            [clojure.spec.alpha :as s]))

(s/def :movement/from :account/name)
(s/def :movement/to :account/name)
(s/def :movement/value ::model.money/money)
(s/def ::movement (s/keys :req [:movement/from
                                :movement/to
                                :movement/value]))

(s/def :transaction/movements (s/coll-of ::movement :min-count 1))

(s/def ::transaction (s/keys :req [:transaction/movements]))

(def datascript-schema
  {:movement/from         {:db/valueType   :db.type/ref
                           :ref-attr       :account/name
                           :db/cardinality :db.cardinality/one
                           :db/doc         "Account from which money is being moved"}
   :movement/to           {:db/valueType   :db.type/ref
                           :ref-attr       :account/name
                           :db/cardinality :db.cardinality/one
                           :db/doc         "Account to which money is being moved"}
   :movement/value        {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/one
                           :db/isComponent true
                           :db/doc         "Money amount that's being moved"}
   :transaction/movements {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many
                           :db/isComponent true
                           :db/doc         "Money movements being made on this transaction"}})

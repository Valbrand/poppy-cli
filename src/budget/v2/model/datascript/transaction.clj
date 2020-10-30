(ns budget.v2.model.datascript.transaction
  (:require [budget.v2.model.money :as model.money]
            [clojure.spec.alpha :as s]))

(def schema
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

(defn ds->movement
  [ds-movement]
  (let [{:movement/keys [from to value]} ds-movement]
    {:movement/from  (:account/name from)
     :movement/to    (:account/name to)
     :movement/value value}))

(defn movement->ds
  [movement]
  (let [{:movement/keys [from to value]} movement]
    {:movement/from [:account/name from]
     :movement/to [:account/name to]
     :movement/value value}))

(defn ds->transaction
  [ds-entity]
  (let [{:transaction/keys [movements]} ds-entity]
    {:transaction/movements (map ds->movement movements)}))

(defn transaction->ds
  [transaction]
  (let [{:transaction/keys [movements]} transaction]
    {:transaction/movements (map movement->ds movements)}))

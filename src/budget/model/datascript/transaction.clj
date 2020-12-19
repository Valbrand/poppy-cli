(ns budget.model.datascript.transaction
  (:require [budget.model.transaction :as model.transaction]
            [budget.model.datascript.money :as model.ds.money]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def schema
  {:movement/account      {:db/valueType   :db.type/ref
                           :ref-attr       :account/name
                           :db/cardinality :db.cardinality/one
                           :db/doc         "Account being affected by the movement"}
   :movement/value        {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/one
                           :db/isComponent true
                           :db/doc         "Money amount that's being moved"}
   :transaction/movements {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many
                           :db/isComponent true
                           :db/doc         "Money movements being made on this transaction"}})

(ds/defn ds->movement :- ::model.transaction/movement
  [ds-movement]
  (let [{:movement/keys [account value]} ds-movement]
    {:movement/account (:account/name account)
     :movement/value   (model.ds.money/ds->money value)}))

(ds/defn movement->ds
  [movement :- ::model.transaction/movement]
  (let [{:movement/keys [account value]} movement]
    {:movement/account [:account/name account]
     :movement/value   (model.ds.money/money->ds value)}))

(ds/defn ds->transaction :- ::model.transaction/transaction
  [ds-entity]
  (let [{:transaction/keys [movements]} ds-entity]
    {:transaction/movements (map ds->movement movements)}))

(ds/defn transaction->ds
  [transaction :- ::model.transaction/transaction]
  (let [{:transaction/keys [movements]} transaction]
    [{:transaction/movements (map movement->ds movements)}]))

(ns budget.v2.model.datascript.account
  (:require [budget.v2.model.account :as model.account]
            [budget.v2.model.datascript.transaction :as model.ds.transaction]
            [budget.v2.model.money :as model.money]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def datascript-schema
  {:account/name {:db/cardinality :db.cardinality/one
                  :db/doc "Account name"
                  :db/unique :db.unique/identity}
   :account/transactions {:db/valueType :db.type/ref
                          :db/cardinality :db.cardinality/many
                          :db/doc "Transactions related to this account"}})

(ds/defn ds->account :- ::model.account/account
  [ds-entity]
  (let [{:account/keys [name transactions]} ds-entity]
    {:account/name name
     :account/transactions (map model.ds.transaction/ds->transaction transactions)}))

(ds/defn account->ds
  [account :- ::model.account/account]
  (let [{:account/keys [name transactions]} account]
    {:account/name name
     :account/transactions (map model.ds.transaction/transaction->ds transactions)}))

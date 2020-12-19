(ns budget.model.datascript.account
  (:require [budget.model.account :as model.account]
            [budget.model.datascript.transaction :as model.ds.transaction]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def schema
  {:account/name {:db/cardinality :db.cardinality/one
                  :db/doc "Account name"
                  :db/unique :db.unique/identity}})

(ds/defn ds->account :- ::model.account/account
  [ds-entity]
  (let [{:account/keys [name transactions]} ds-entity]
    {:account/name name
     :account/transactions (map model.ds.transaction/ds->transaction transactions)}))

(ds/defn account->ds
  [account :- ::model.account/account]
  (let [{:account/keys [name transactions]} account]
    (into
     [{:account/name name}]
     (flatten (map model.ds.transaction/transaction->ds transactions)))))

(account->ds #:account{:name "a" :transactions [1]})
(ns budget.model.datascript.account
  (:require [budget.model.account :as model.account]
            [budget.utils :as utils]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def schema
  {:account/name {:db/cardinality :db.cardinality/one
                  :db/doc "Account name"
                  :db/unique :db.unique/identity}})

(ds/defn ds->account :- ::model.account/account
  [ds-entity]
  (let [{:account/keys [name] :meta/keys [created-at]} ds-entity]
    (utils/assoc-if-some {:account/name name}
                         :meta/created-at
                         created-at)))

(ds/defn account->ds
  [account :- ::model.account/account]
  (let [{:account/keys [name] :meta/keys [created-at]} account]
    [(utils/assoc-if-some {:account/name name}
                          :meta/created-at
                          created-at)]))
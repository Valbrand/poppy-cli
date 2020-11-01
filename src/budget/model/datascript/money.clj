(ns budget.v2.model.datascript.money
  (:require [budget.v2.model.money :as model.money]
            [net.danielcompton.defn-spec-alpha :as ds]))

(def schema
  {:money/value    {:db/cardinality :db.cardinality/one
                    :db/doc         "Money amount"}
   :money/currency {:db/cardinality :db.cardinality/one
                    :db/doc         "Currency tied to the amount"}})

(ds/defn ds->money :- ::model.money/money
  [ds-entity]
  (let [{:money/keys [value currency]} ds-entity]
    #:money {:value    value
             :currency currency}))

(ds/defn money->ds
  [money :- ::model.money/money]
  (let [{:keys [value currency]} money]
    #:money {:value    value
             :currency currency}))

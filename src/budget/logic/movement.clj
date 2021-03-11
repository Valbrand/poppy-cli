(ns budget.logic.movement
  (:require [budget.logic.account :as logic.account]
            [budget.model.account :as model.account]
            [budget.model.transaction :as model.transaction]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn matches-account? :- boolean?
  [account-name :- :account/name
   movement :- ::model.transaction/movement]
  (= account-name (:movement/account movement)))

(ds/defn matches-account-type? :- boolean?
  [account-types :- (s/coll-of ::model.account/account-type :kind set?)
   movement :- ::model.transaction/movement]
  (contains? account-types
             (logic.account/account-name->account-type (:movement/account movement))))
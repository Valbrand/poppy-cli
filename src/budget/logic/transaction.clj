(ns budget.logic.transaction
  (:require [budget.logic.account :as logic.account]
            [budget.logic.movement :as logic.movement]
            [budget.model.account :as model.account]
            [budget.model.transaction :as model.transaction]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn movements-for-account :- (s/coll-of ::model.transaction/movement)
  [account-name :- :account/name
   transaction :- ::model.transaction/transaction]
  (->> transaction
       :transaction/movements
       (filter (partial logic.movement/matches-account? account-name))))

(ds/defn movements-for-account-types :- (s/coll-of ::model.transaction/movement)
  [account-types :- (s/coll-of ::model.account/account-type :kind set?)
   transaction :- ::model.transaction/transaction]
  (->> transaction
       :transaction/movements
       (filter (partial logic.movement/matches-account-type? account-types))))
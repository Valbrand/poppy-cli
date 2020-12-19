(ns budget.model.transaction
  (:require [budget.model.money :as model.money]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def :movement/account :account/name)
(s/def :movement/value ::model.money/money)
(s/def ::movement (s/keys :req [:movement/account
                                :movement/value]))

(s/def :transaction/movements (s/coll-of ::movement :min-count 1))

(s/def ::transaction (s/keys :req [:transaction/movements]))

(ds/defn new-movement :- ::movement
  [account :- :movement/account
   value :- :movement/value]
  #:movement {:account account
              :value   value})

(defn new-transaction
  [movements]
  #:transaction {:movements movements})

(ns budget.v2.entries.new-transaction.spec
  (:require [budget.v2.entries.core.spec :as entries.spec]
            [budget.v2.model.money :as money]
            [clojure.spec.alpha :as s]))

(s/def :new-movement/account ::entries.spec/account-name)
(s/def :new-movement/amount ::money/value)
(s/def :new-movement/currency ::entries.spec/currency)

(s/def ::movement (s/keys :req [:new-movement/account
                                :new-movement/amount
                                :new-movement/currency]))

(s/def :new-transaction/movements (s/coll-of ::movement :min-count 2))

(s/def ::new-transaction (s/keys :req [:meta/created-at :new-transaction/movements]
                                 :opt [:meta/description :meta/tags]))

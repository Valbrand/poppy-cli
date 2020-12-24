(ns budget.entries.new-transaction.spec
  (:require [budget.entries.core.spec :as entries.spec]
            [budget.model.money :as model.money]
            [clojure.spec.alpha :as s]))

(s/def :new-movement/account ::entries.spec/account-name)
(s/def :new-movement/value ::model.money/money)

(s/def ::movement (s/keys :req [:new-movement/account
                                :new-movement/value]))

(s/def :new-transaction/movements (s/coll-of ::movement :min-count 2))

(s/def ::new-transaction (s/keys :req [:meta/created-at :new-transaction/movements]
                                 :opt [:meta/description :meta/tags]))

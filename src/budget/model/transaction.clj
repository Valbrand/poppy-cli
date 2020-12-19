(ns budget.model.transaction
  (:require [budget.model.meta]
            [budget.model.money :as model.money]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def :movement/account :account/name)
(s/def :movement/value ::model.money/money)
(s/def ::movement (s/keys :req [:movement/account
                                :movement/value]))

(s/def :transaction/movements (s/coll-of ::movement :min-count 1))

(s/def :transaction/meta (s/keys :opt [:meta/created-at :meta/description :meta/tags]))
(s/def ::transaction (s/keys :req [:transaction/movements] :opt [:meta/created-at :meta/description :meta/tags]))

(ds/defn new-movement :- ::movement
  [account :- :movement/account
   value :- :movement/value]
  #:movement {:account account
              :value   value})

(ds/defn new-transaction :- ::transaction
  ([movements :- :transaction/movements]
   (new-transaction movements {}))
  ([movements :- :transaction/movements
    meta-attrs :- :transaction/meta]
   (let [{:meta/keys [created-at description tags]} meta-attrs]
     #:transaction {:movements        movements
                    :meta/created-at  created-at
                    :meta/description description
                    :meta/tags        tags})))

(ns budget.reporter.account-balances
  (:require [budget.model.account :as model.account]
            [budget.model.money :as model.money]
            [budget.model.transaction :as model.transaction]
            [budget.state.protocols :as state.protocols]
            [budget.utils :as utils]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::balances (s/coll-of ::model.money/money))
(s/def ::report (s/map-of :account/name ::balances))

(ds/defn movements-for-account :- (s/coll-of ::model.transaction/movement)
  [account-name :- :account/name
   transaction :- ::model.transaction/transaction]
  (->> transaction
       :transaction/movements
       (filter #(= account-name (:movement/account %)))))

(ds/defn aggregate-monetary-values :- (s/coll-of ::model.money/money)
  [values :- (s/coll-of ::model.money/money)]
  (->> values
       (group-by :currency)
       vals
       (map #(reduce (fn [result {:keys [value]}]
                       (update result :value + value))
                     %))))

(ds/defn aggregate-movements-value :- (s/coll-of ::model.money/money)
  [movements :- (s/coll-of ::model.transaction/movement)]
  (->> movements
       (map :movement/value)
       aggregate-monetary-values))

(ds/defn transactions->balance-total :- (s/coll-of ::model.money/money)
  [account-name :- :account/name
   transactions :- (s/coll-of ::model.transaction/transaction)]
  (->> transactions
       (map (partial movements-for-account account-name))
       (map aggregate-movements-value)
       flatten
       aggregate-monetary-values))

(defn account-type
  [account-name]
  (-> account-name (str/split #"/") first))

(def account-presentation-order
  (->> ["equity" "assets"]
       (map-indexed #(vector %2 %1))
       (into {})))

(ds/defn report :- ::report
  [accounts :- (s/coll-of ::model.account/account)
   transactions-for-account :- (s/fspec :args (s/cat :account-name :account/name)
                                        :ret (s/coll-of ::model.transaction/transaction))]
  (->> accounts
       (map (fn [{:account/keys [name]}]
              [name (->> name
                         transactions-for-account
                         (transactions->balance-total name))]))
       (group-by (comp account-type first))
       (map (fn [[account-type reports]]
              [account-type (into {} (sort-by first reports))]))
       (into {})))

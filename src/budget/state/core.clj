(ns budget.v2.state.core
  (:require [budget.v2.state.protocols :as state.protocols]
            [budget.v2.model.datascript.account :as model.ds.account]
            [budget.v2.model.datascript.money :as model.ds.money]
            [budget.v2.model.datascript.transaction :as model.ds.transaction]
            [budget.v2.utils :as utils]
            [datascript.core :as db]))

(defprotocol IState
  (validate-initialization! [this]))

(defrecord State
  []

  IState
  (validate-initialization!
    [this]
    (when-not (:state/initialized? this)
      (throw (ex-info "State should be created by calling new-state" {}))))

  state.protocols/AccountStore
  (account-by-name
    [{:state/keys [connection] :as this} account-name]
    (validate-initialization! this)
    (-> (db/entity @connection [:account/name account-name])
        model.ds.account/ds->account))
  (put-account
    [{:state/keys [connection] :as this} account]
    (validate-initialization! this)
    (->> account
         model.ds.account/account->ds
         (db/transact! connection)))

  state.protocols/TransactionStore
  (transactions-by-account-name
    [{:state/keys [connection] :as this} account-name]
    (validate-initialization! this)
    (->> (db/entity @connection [:account/name account-name])
         model.ds.account/ds-transactions
         (map model.ds.transaction/ds->transaction)))
  (put-transaction
    [{:state/keys [connection] :as this} transaction]
    (validate-initialization! this)))

(defn new-datascript-state
  ([schema] (new-datascript-state schema {}))
  ([schema opts]
   (let [connection (db/create-conn schema)]
     (-> (map->State opts)
         (assoc :state/connection connection)
         (assoc :state/initialized? true)))))

(comment
  (do
    (require '[datascript.core :as d])
    
    (def schema (utils/safe-merge model.ds.account/schema
                                  model.ds.money/schema
                                  model.ds.transaction/schema))
    
    (def state (new-datascript-state schema)))
  
  state)
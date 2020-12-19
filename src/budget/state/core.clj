(ns budget.state.core
  (:require [budget.state.protocols :as state.protocols]
            [budget.model.datascript.account :as model.ds.account]
            [budget.model.datascript.money :as model.ds.money]
            [budget.model.datascript.transaction :as model.ds.transaction]
            [budget.utils :as utils]
            [datascript.core :as db]))

(defprotocol IState
  (validate-initialization! [this]))

(defrecord State
  []

  IState
  (validate-initialization!
    [this]
    (when-not (:state/initialized? this)
      (throw (ex-info "State should be created by calling new-datascript-state" {}))))

  state.protocols/AccountStore
  (account-by-name
    [{:state/keys [connection] :as this} account-name]
    (validate-initialization! this)
    (-> (db/entity @connection [:account/name account-name])
        model.ds.account/ds->account))
  (put-account!
    [{:state/keys [connection] :as this} account]
    (validate-initialization! this)
    (->> account
         model.ds.account/account->ds
         (db/transact! connection))
    this)

  state.protocols/TransactionStore
  (transactions-by-account-name
    [{:state/keys [connection] :as this} account-name]
    (validate-initialization! this))
  (put-transaction!
    [{:state/keys [connection] :as this} transaction]
    (validate-initialization! this)
    (->> transaction
         model.ds.transaction/transaction->ds
         (db/transact! connection))
    this)
  (put-transactions!
   [{:state/keys [connection] :as this} transactions]
   (validate-initialization! this)
   (db/transact! connection
                 (->> transactions
                      (map model.ds.transaction/transaction->ds)
                      flatten
                      (into [])))
   this))

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
    (require '[budget.model.datascript.core :as ds.core])
    
    (def state (new-datascript-state ds.core/schema)))
  
  (d/datoms @(:state/connection state) :eavt)
  (d/transact! (:state/connection state) [#:account{:name "equity/initial-balance"}
                                          #:account{:name "assets/nuconta"}
                                          #:transaction{:movements
                                                        [#:movement{:from [:account/name "equity/initial-balance"]
                                                                    :to [:account/name "assets/nuconta"]
                                                                    :value #:money{:value 100N, :currency :BRL}}]}]))
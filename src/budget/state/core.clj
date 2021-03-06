(ns budget.state.core
  (:require [budget.state.protocols :as state.protocols]
            [budget.model.datascript.account :as model.ds.account]
            [budget.model.datascript.money :as model.ds.money]
            [budget.model.datascript.transaction :as model.ds.transaction]
            [budget.utils :as utils]
            [clojure.string :as str]
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
  (all-accounts
    [{:state/keys [connection] :as this}]
    (validate-initialization! this)
    (->> (db/q '{:find [(pull ?account [*])]
                 :where [[?account :account/name _]]}
               @connection)
         (map (comp model.ds.account/ds->account first))))
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
    (validate-initialization! this)
    (->> (db/q '{:find  [(pull ?transaction [* {:transaction/movements [* {:movement/account [:account/name]}]}])]
                 :in    [$ ?account-name]
                 :where [[?account :account/name ?account-name]
                         [?movement :movement/account ?account]
                         [?transaction :transaction/movements ?movement]]}
               @connection account-name)
         (map (comp model.ds.transaction/ds->transaction first))))
  (transactions-by-account-types
   [{:state/keys [connection] :as this} account-types]
   (validate-initialization! this)
   (->> (db/q '{:find [(pull ?transaction [* {:transaction/movements [* {:movement/account [:account/name]}]}])]
                :in [$ [?account-type ...]]
                :where [[?account :account/name ?account-name]
                        [(clojure.string/starts-with? ?account-name ?account-type)]
                        [?movement :movement/account ?account]
                        [?transaction :transaction/movements ?movement]]}
              @connection account-types)
        (map (comp model.ds.transaction/ds->transaction first))))
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
   this)

  state.protocols/ExchangeRateStore
  (exchange-rates
   [{:state/keys [exchange-rates]}]
   exchange-rates)
  (set-exchange-rates!
   [this exchange-rates]
   (update this :state/exchange-rates merge exchange-rates)))

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

    (def state (new-datascript-state ds.core/schema))

    (d/transact! (:state/connection state) [#:account{:name "equity/initial-balance"}
                                            #:account{:name "assets/nuconta"}
                                            #:transaction{:movements
                                                          [#:movement{:account [:account/name "equity/initial-balance"]
                                                                      :value #:money{:value -100N, :currency :BRL}}
                                                           #:movement{:account [:account/name "assets/nuconta"]
                                                                      :value #:money{:value 100N, :currency :BRL}}]}]))

  (require '[clojure.string :as str])

  (db/q '{:find [(pull ?transaction [*])]
          :in [$ [?account-type ...]]
          :where [[?account :account/name ?account-name]
                  [(str/starts-with? ?account-name ?account-type)]
                  [?movement :movement/account ?account]
                  [?transaction :transaction/movements ?movement]]}
        @(:state/connection state)
        #{"equity"})

  (db/q '{:find  [(pull ?account [* {:transaction/movements [* {:movement/account [:account/name]}]}])]
          :in    [$ ?account-name]
          :where [[?account :account/name ?account-name]]}
        @(:state/connection state) "assets/nuconta")

  (state.protocols/all-accounts state)
  (state.protocols/transactions-by-account-name state "assets/nuconta")

  (d/datoms @(:state/connection state) :eavt))
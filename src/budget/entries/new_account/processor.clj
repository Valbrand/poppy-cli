(ns budget.entries.new-account.processor
  (:require [budget.entries.new-account.spec :as spec]
            [budget.model.account :as model.account]
            [budget.model.money :as model.money]
            [budget.model.transaction :as model.transaction]
            [budget.state.protocols :as state.protocols]
            [budget.time :as time]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn entry->initial-transactions :- :account/transactions
  [entry :- ::spec/new-account]
  (if (contains? entry :new-account/balance)
    (let [{:new-account/keys [name balance currency]} entry]
      [(model.transaction/new-transaction
        [(model.transaction/new-movement "equity/initial-balance"
                                         (model.money/money (- balance) currency))
         (model.transaction/new-movement name
                                         (model.money/money balance currency))]
        entry)])
    []))

(ds/defn ^:private entry->model :- ::model.account/account
  [entry :- ::spec/new-account]
  {:account      (model.account/new-account (:new-account/name entry)
                                            entry)
   :transactions (entry->initial-transactions entry)})

(ds/defn process-entry! :- ::state.protocols/state
  [state :- ::state.protocols/state
   entry :- ::spec/new-account]
  (let [{:keys [account transactions]} (entry->model entry)]
    (state.protocols/put-account! state account)
    (state.protocols/put-transactions! state transactions)))

(def config
  {:new-account {:entry-processor process-entry!}})

(ds/defn initialize-state! :- ::state.protocols/state
  [state :- ::state.protocols/state]
  (state.protocols/put-account! state
                                (model.account/new-account "equity/initial-balance"
                                                           {:meta/created-at time/beginning-of-time})))
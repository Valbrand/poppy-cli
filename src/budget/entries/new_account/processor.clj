(ns budget.entries.new-account.processor
  (:require [budget.entries.new-account.spec :as spec]
            [budget.model.account :as model.account]
            [budget.model.money :as model.money]
            [budget.model.transaction :as model.transaction]
            [budget.state.protocols :as state.protocols]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn entry->initial-transactions :- :account/transactions
  [entry :- ::spec/new-account]
  (if (contains? entry :new-account/balance)
    (let [{:new-account/keys [name balance currency]} entry]
      [(model.transaction/new-transaction
        [(model.transaction/new-movement "equity/initial-balance"
                                         (model.money/money (- balance) currency))
         (model.transaction/new-movement name
                                         (model.money/money balance currency))])])
    []))

(ds/defn ^:private entry->model :- ::model.account/account
  [entry :- ::spec/new-account]
  (model.account/new-account (:new-account/name entry)
                             (entry->initial-transactions entry)))

(ds/defn process-entry! :- ::state.protocols/state
  [state :- ::state.protocols/state
   entry :- ::spec/new-account]
  (->> entry
       entry->model
       (state.protocols/put-account! state)))

(def config
  {:new-account {:entry-processor process-entry!}})

(ds/defn initialize-state! :- ::state.protocols/state
  [state :- ::state.protocols/state]
  (state.protocols/put-account! state 
                               (model.account/new-account "equity/initial-balance" [])))
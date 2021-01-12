(ns budget.entries.new-account.processor
  (:require [budget.entries.new-account.spec :as spec]
            [budget.logic.account :as logic.account]
            [budget.model.account :as model.account]
            [budget.model.money :as model.money]
            [budget.model.transaction :as model.transaction]
            [budget.state.protocols :as state.protocols]
            [budget.time :as time]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn entry->initial-transactions :- (s/coll-of ::model.transaction/transaction)
  [account-name :- :account/name
   entry :- ::spec/new-account]
  (if (contains? entry :new-account/balance)
    (let [{:new-account/keys [balance currency]} entry]
      [(model.transaction/new-transaction
        [(model.transaction/new-movement "equity/initial-balance"
                                         (model.money/money (- balance) currency))
         (model.transaction/new-movement account-name
                                         (model.money/money balance currency))]
        entry)])
    []))

(ds/defn ^:private entry->accounts :- (s/coll-of ::model.account/account)
  [entry :- ::spec/new-account]
  (if (#{"expenses" "budget"} (-> entry :new-account/name logic.account/account-name->account-type))
    (let [account-identifier (logic.account/account-name->identifier (:new-account/name entry))]
      (->> ["expenses" "budget"]
           (map #(str % "/" account-identifier))
           (map #(model.account/new-account % entry))))
    [(model.account/new-account (:new-account/name entry)
                                entry)]))

(ds/defn ^:private entry->model :- ::model.account/account
  [entry :- ::spec/new-account]
  (let [accounts (entry->accounts entry)]
    {:accounts     accounts
     :transactions (->> accounts
                        (map :account/name)
                        (map #(entry->initial-transactions % entry))
                        flatten)}))

(ds/defn process-entry! :- ::state.protocols/state
  [state :- ::state.protocols/state
   entry :- ::spec/new-account]
  (let [{:keys [accounts transactions]} (entry->model entry)]
    (-> (reduce #(state.protocols/put-account! %1 %2) state accounts)
        (state.protocols/put-transactions! transactions))))

(def config
  {:new-account {:entry-processor process-entry!}})

(ds/defn initialize-state! :- ::state.protocols/state
  [state :- ::state.protocols/state]
  (state.protocols/put-account! state
                                (model.account/new-account "equity/initial-balance"
                                                           {:meta/created-at time/beginning-of-time})))
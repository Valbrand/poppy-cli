(ns budget.entries.new-transaction.processor
  (:require [budget.entries.new-transaction.spec :as spec]
            [budget.model.transaction :as model.transaction]
            [budget.state.protocols :as state.protocols]
            [budget.utils :as utils]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn ^:private entry->model :- ::model.transaction/transaction
  [entry :- ::spec/new-transaction]
  (let [model-movements (->> entry
                             :new-transaction/movements
                             (map (fn [{:new-movement/keys [account value]}]
                                    (model.transaction/new-movement account value))))]
    (model.transaction/new-transaction model-movements entry)))

(ds/defn process-entry! :- ::state.protocols/state
  [state :- ::state.protocols/state
   entry :- ::spec/new-transaction]
  (->> entry
       entry->model
       (state.protocols/put-transaction! state)))

(def config
  {:new-transaction {:entry-processor process-entry!}})

(ds/defn initialize-state! :- ::state.protocols/state
  [state :- ::state.protocols/state]
  state)
(ns budget.entries.set-exchange-rate.processor
  (:require [budget.entries.set-exchange-rate.spec :as spec]
            [budget.state.protocols :as state.protocols]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn ^:private entry->model
  [entry :- ::spec/new-transaction]
  (:set-exchange-rates/rates entry))

(ds/defn process-entry! :- ::state.protocols/state
  [state :- ::state.protocols/state
   entry :- ::spec/set-exchange-rate]
  (->> entry
       entry->model
       (state.protocols/set-exchange-rates! state)))

(def config
  {:set-exchange-rates {:entry-processor process-entry!}})

(ds/defn initialize-state! :- ::state.protocols/state
  [state :- ::state.protocols/state]
  state)
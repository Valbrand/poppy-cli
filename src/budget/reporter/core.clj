(ns budget.reporter.core
  (:require [budget.reporter.account-balances :as reporter.account-balances]
            [budget.state.protocols :as state.protocols]
            [plumbing.core :refer [fnk]]
            [plumbing.graph :as graph]))

(def reports-graph
  (graph/lazy-compile
   {:all-accounts
    (fnk [state]
      (state.protocols/all-accounts state))

    :transactions-for-account
    (fnk [state]
      (partial state.protocols/transactions-by-account-name state))

    :account-balances
    (fnk [all-accounts transactions-for-account]
      (reporter.account-balances/report all-accounts transactions-for-account))}))

(def ^:private presenters
  {:account-balances reporter.account-balances/present!})

(defn present!
  [report-type report]
  (let [present-report! (get presenters report-type)]
    (assert (some? present-report!) (format "Presenter not found for type %s" report-type))
    (present-report! report)))
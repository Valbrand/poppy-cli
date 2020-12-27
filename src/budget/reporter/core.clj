(ns budget.reporter.core
  (:require [budget.reporter.account-balances :as reporter.account-balances]
            [budget.reporter.budget-allocation :as reporter.budget-allocation]
            [budget.reporter.current-net-worth :as reporter.current-net-worth]
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
      (reporter.account-balances/report all-accounts transactions-for-account))

    :assets-liabilities-balances
    (fnk [account-balances]
      account-balances)

    :budget-goal-balances
    (fnk [account-balances]
      account-balances)

    :current-net-worth
    (fnk [account-balances]
      (reporter.current-net-worth/report account-balances))

    :budget-allocation
    (fnk [account-balances current-net-worth]
      (reporter.budget-allocation/report account-balances current-net-worth))}))

(def ^:private presenters
  {:account-balances            reporter.account-balances/present!
   :assets-liabilities-balances (partial reporter.account-balances/present! {:account-types ["assets" "liabilities"]})
   :budget-goal-balances        (partial reporter.account-balances/present! {:account-types        ["budget" "goal"]
                                                                             :report-title         "Budget allocations"
                                                                             :omit-empty-accounts? true})
   :current-net-worth           reporter.current-net-worth/present!
   :budget-allocation           reporter.budget-allocation/present!})

(defn present!
  [report-type report]
  (let [present-report! (get presenters report-type)]
    (assert (some? present-report!) (format "Presenter not found for type %s" report-type))
    (present-report! report)))
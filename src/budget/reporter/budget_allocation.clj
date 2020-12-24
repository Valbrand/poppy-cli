(ns budget.reporter.budget-allocation
  (:require [budget.logic.account :as logic.account]
            [budget.logic.money :as logic.money]
            [budget.model.account :as model.account]
            [budget.model.money :as model.money]
            [budget.model.transaction :as model.transaction]
            [budget.reporter.account-balances :as reporter.account-balances]
            [budget.reporter.common :as reporter.common :refer [indent println']]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::allocation-situation #{:overbudgeted :not-allocated})
(s/def ::report (s/map-of ::allocation-situation (s/coll-of ::model.money/money)))

(ds/defn ^:private balances-report->balances :- (s/coll-of ::model.money/money)
  [account-balances :- ::reporter.account-balances/report
   account-types :- (s/coll-of ::model.account/account-type)]
  (->> (select-keys account-balances account-types)
       vals
       (map vals)
       flatten
       logic.money/aggregate-monetary-values))

(ds/defn report :- ::report
  [account-balances :- ::reporter.account-balances/report]
  (let [balances (balances-report->balances account-balances ["equity" "incomes" "budget" "goal"])
        not-allocated (->> balances
                           (filter logic.money/negative-value?)
                           (map logic.money/abs-value))
        overbudgeted (->> balances
                          (filter logic.money/positive-value?)
                          (map logic.money/abs-value))]
    (into {}
          (filter (comp seq second))
          {:overbudgeted  overbudgeted
           :not-allocated not-allocated})))

(ds/defn present!
  [report :- ::report]
  (let [{:keys [overbudgeted not-allocated]} report]
    (println' "Budget allocation:")
    (indent 2
      (when (empty? report)
        (println' "All of your money is allocated into budgets and goals."))
      (when (seq overbudgeted)
        (println' "Overbudgeted (You allocated more than you have into budgets and goals):")
        (reporter.common/print-monetary-values overbudgeted))
      (when (seq not-allocated)
        (println' "Not allocated (You allocated less to budgets than you have gained as incomes/equity):")
        (reporter.common/print-monetary-values not-allocated)))))

(comment
  model.account/valid-account-types
  (present! (report {"liabilities"
           {"liabilities/cc-c6" '({:value 0M, :currency :BRL})
            "liabilities/lending" '({:value 0M, :currency :BRL})
            "liabilities/lending-current-month" '({:value 0M, :currency :BRL})
            "liabilities/nubank-closed-bill" '({:value 0M, :currency :BRL})
            "liabilities/nubank-future" '({:value -3401.63M, :currency :BRL})
            "liabilities/nubank-open-bill" '({:value -5029.51M, :currency :BRL})}
           "assets"
           {"assets/Ame" '({:value 0M, :currency :BRL})
            "assets/IOUs" '({:value 27.11M, :currency :BRL})
            "assets/VA" '({:value 28.87M, :currency :BRL})
            "assets/VR" '({:value 10.82M, :currency :BRL})
            "assets/nuconta" '({:value 692.26M, :currency :BRL})
            "assets/nuconta-buckets" '({:value 2079.14M, :currency :BRL})
            "assets/picpay" '({:value 21836.41M, :currency :BRL})}
           "goal" {"goal/niceties" [{:value 16243.47M :currency :BRL}]}
           "equity" {"equity/initial-balance" '({:value -16243.47M, :currency :BRL})}})))
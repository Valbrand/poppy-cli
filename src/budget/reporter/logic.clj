(ns budget.reporter.logic
  (:require [budget.logic.money :as logic.money]
            [budget.model.money :as model.money]
            [budget.reporter.account-balances :as reporter.account-balances]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn net-worth :- (s/coll-of ::model.money/money)
  [account-balances :- ::reporter.account-balances/report]
  (->> account-balances
       (filter (comp #{"assets" "liabilities"} first))
       (map (comp vals second))
       flatten
       logic.money/aggregate-monetary-values))
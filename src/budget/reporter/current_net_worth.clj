(ns budget.reporter.current-net-worth
  (:require [budget.logic.money :as logic.money]
            [budget.model.money :as model.money]
            [budget.reporter.account-balances :as reporter.account-balances]
            [budget.reporter.common :refer [println' indent]]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::report (s/coll-of ::model.money/money))

(ds/defn report :- ::report
  [account-balances :- ::reporter.account-balances/report]
  (->> account-balances
       (filter (comp #{"assets" "liabilities"} first))
       (map (comp vals second))
       flatten
       logic.money/aggregate-monetary-values))

(ds/defn present!
  [report :- ::report]
  (let [formatted-balances (map logic.money/money->string report)
        max-length (count (apply max-key count formatted-balances))
        balance-format-string (str "%" max-length "s")]
    (println' "Net worth:")
    (indent 2
      (doseq [balance formatted-balances]
        (println' (format balance-format-string balance))))))

(comment
  (present! (report {"assets" {"assets/nuconta" [{:value 500M :currency :BRL}]
                               "assets/nuconta2" [{:value -1500.75M :currency :USD}]}
                     "liabilities" {"liabilities/cc" [{:value -400M :currency :BRL}]}
                     "equity" {"assets/nuconta" [{:value 100M :currency :BRL}]}})))
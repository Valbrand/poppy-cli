(ns budget.reporter.consolidated-net-worth
  (:require [budget.logic.money :as logic.money]
            [budget.model.money :as model.money]
            [budget.reporter.common :as reporter.common :refer [println' indent]]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::report (s/coll-of ::model.money/money))

(ds/defn report :- ::report
  [net-worth :- (s/coll-of ::model.money/money)
   exchange-rates :- (s/map-of ::model.money/currency ::model.money/money)]
  (->> net-worth
       (map #(logic.money/convert % exchange-rates))
       logic.money/aggregate-monetary-values))

(ds/defn present!
  [report :- ::report]
  (println' "Net worth:")
  (indent 2
    (reporter.common/print-monetary-values report)))

(comment
  (present! (report {"assets" {"assets/nuconta" [{:value 500M :currency :BRL}]
                               "assets/nuconta2" [{:value -1500.75M :currency :USD}]}
                     "liabilities" {"liabilities/cc" [{:value -400M :currency :BRL}]}
                     "equity" {"assets/nuconta" [{:value 100M :currency :BRL}]}})))
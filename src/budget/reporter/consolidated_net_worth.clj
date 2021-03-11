(ns budget.reporter.consolidated-net-worth
  (:require [budget.logic.money :as logic.money]
            [budget.model.money :as model.money]
            [budget.reporter.common :as reporter.common :refer [println' indent]]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::base-net-worth (s/coll-of ::model.money/money))
(s/def ::consolidated-net-worth (s/coll-of ::model.money/money))
(s/def ::report (s/keys :req-un [::base-net-worth ::consolidated-net-worth]))

(ds/defn report :- ::report
  [net-worth :- (s/coll-of ::model.money/money)
   exchange-rates :- (s/map-of ::model.money/currency ::model.money/money)]
  {:base-net-worth         net-worth
   :consolidated-net-worth (->> net-worth
                                (map #(logic.money/convert % exchange-rates))
                                logic.money/aggregate-monetary-values)})

(ds/defn present!
  [report :- ::report]
  (let [{:keys [base-net-worth consolidated-net-worth]} report]
    (println' "Net worth:")
    (indent 2
      (reporter.common/print-monetary-values base-net-worth))
    (println' "Consolidated net worth:")
    (indent 2
      (reporter.common/print-monetary-values consolidated-net-worth))))

(comment
  (present! (report [{:value 100M :currency :BRL}
                     {:value 50M :currency :USD}]
                    {:USD {:value 5.7M :currency :BRL}})))
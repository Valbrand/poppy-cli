(ns budget.logic.money
  (:require [budget.model.money :as model.money]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn aggregate-monetary-values :- (s/coll-of ::model.money/money)
  [values :- (s/coll-of ::model.money/money)]
  (->> values
       (group-by :currency)
       vals
       (map #(reduce (fn [result {:keys [value]}]
                       (update result :value + value))
                     %))))

(ds/defn money->string :- string?
  [money :- ::model.money/money]
  (let [{:keys [value currency]} money]
    (format "%.02f %s" value (name currency))))

(ns budget.v2.entries.new-transaction.validator
  (:require [budget.v2.entries.new-transaction.spec :as spec]
            [budget.v2.model.money :as money]
            [budget.v2.utils :as utils]
            [budget.v2.validator.core :as validator]
            [budget.v2.validator.spec :as validator.spec]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::validation-report (validator.spec/validation-report-spec ::spec/new-transaction))

(ds/defn ^:private total-amount :- :movement/amount
  [movements :- (s/coll-of ::spec/movement)]
  (->> movements
       (map :movement/amount)
       (reduce money/add money/ZERO)))

(ds/defn ^:private outstanding-currency-change-error :- ::validator.spec/error
  [currency :- :movement/currency
   outstanding-total :- :movement/amount]
  {:message (format "Total balance change for all currencies in a transaction must sum to zero. %s currency is changing by %s" currency outstanding-total)
   :details {:currency currency
             :outstanding-value outstanding-total}})

(ds/defn ^:private outstanding-currency-change :- ::validator.spec/error
  [input :- (s/tuple :movement/currency :movement/amount)]
  (let [[currency value] input]
    (when-not (money/zero-value? value)
      (outstanding-currency-change-error currency value))))

(ds/defn ^:private validate-movements! :- ::validation-report
  [validation-report :- ::validation-report]
  (let [{transaction :entry} validation-report
        movement-totals-by-currency (->> transaction
                                         :transaction/movements
                                         (group-by :movement/currency)
                                         (utils/map-vals total-amount))
        outstanding-currency-change (delay
                                     (some outstanding-currency-change movement-totals-by-currency))]
    (cond-> validation-report
      @outstanding-currency-change
      (validator/add-error @outstanding-currency-change))))

(ds/defn validate :- ::validation-report
  [transaction :- ::spec/new-transaction]
  (-> transaction
      validator/base-validation-report
      validate-movements!))

(def config
  {:new-transaction {:entry-validator validate}})
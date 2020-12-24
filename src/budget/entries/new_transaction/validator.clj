(ns budget.entries.new-transaction.validator
  (:require [budget.entries.new-transaction.spec :as spec]
            [budget.logic.account :as logic.account]
            [budget.logic.money :as logic.money]
            [budget.model.account :as model.account]
            [budget.validator.core :as validator]
            [budget.validator.spec :as validator.spec]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::validation-report (validator.spec/validation-report-spec ::spec/new-transaction))

(ds/defn ^:private outstanding-currency-change-error :- ::validator.spec/error
  [currency :- :movement/currency
   outstanding-total :- :new-movement/amount]
  {:message (format "Total balance change for all currencies in a transaction must sum to zero. %s currency is changing by %s" currency outstanding-total)
   :details {:currency currency
             :outstanding-value outstanding-total}})

(ds/defn movements-for-account-types :- (s/coll-of ::spec/movement)
  [account-types :- ::model.account/account-type
   movements :- (s/coll-of ::spec/movement)]
  (->> movements
       (filter (comp account-types logic.account/account-name->account-type :new-movement/account))))

(ds/defn validate-zero-sum :- ::validation-report
  [validation-report :- ::validation-report]
  (let [value-deltas (->> validation-report
                          :entry
                          :new-transaction/movements
                          (movements-for-account-types #{"assets" "liabilities" "equity" "incomes"})
                          (map :new-movement/value)
                          logic.money/aggregate-monetary-values)]
    (reduce (fn [report {:keys [value currency]}]
              (if (zero? value)
                report
                (validator/add-error report (outstanding-currency-change-error currency value))))
            validation-report
            value-deltas)))

(ds/defn validate :- ::validation-report
  [transaction :- ::spec/new-transaction]
  (-> transaction
      validator/base-validation-report
      validate-zero-sum))

(def config
  {:new-transaction {:entry-validator validate}})
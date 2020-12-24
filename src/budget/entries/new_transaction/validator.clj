(ns budget.entries.new-transaction.validator
  (:require [budget.entries.new-transaction.spec :as spec]
            [budget.logic.account :as logic.account]
            [budget.logic.money :as logic.money]
            [budget.model.account :as model.account]
            [budget.model.money :as model.money]
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

(ds/defn ^:private movements-for-account-types :- (s/coll-of ::spec/movement)
  [account-types :- ::model.account/account-type
   movements :- (s/coll-of ::spec/movement)]
  (->> movements
       (filter (comp account-types logic.account/account-name->account-type :new-movement/account))))

(ds/defn ^:private validate-zero-sum :- ::validation-report
  [validation-report :- ::validation-report]
  (let [value-deltas (->> (get-in validation-report [:entry :new-transaction/movements])
                          (movements-for-account-types #{"assets" "liabilities" "equity" "incomes"})
                          (map :new-movement/value)
                          logic.money/aggregate-monetary-values)]
    (reduce (fn [report {:keys [value currency]}]
              (if (zero? value)
                report
                (validator/add-error report (outstanding-currency-change-error currency value))))
            validation-report
            value-deltas)))

(ds/defn ^:private deltas-by-account-types :- (s/coll-of ::model.money/money)
  [account-types :- (s/coll-of ::model.account/account-type)
   movements :- (s/coll-of ::spec/movement)]
  (->> movements
       (movements-for-account-types account-types)
       (map :new-movement/value)
       logic.money/aggregate-monetary-values))

(ds/defn ^:private budget-allocation-error :- ::validator.spec/error
  [currency :- ::model.money/currency
   budget-delta :- ::model.money/value
   net-worth-delta :- ::model.money/value]
  {:message "Your net-worth and your budget allocation changed by different amounts"
   :details {:budget-delta    (logic.money/money->string (model.money/money budget-delta currency))
             :net-worth-delta (logic.money/money->string (model.money/money net-worth-delta currency))}})

(ds/defn validate-budget-allocations :- ::validation-report
  [validation-report :- ::validation-report]
  (let [movements (get-in validation-report [:entry :new-transaction/movements])
        net-worth-deltas (group-by :currency (deltas-by-account-types #{"assets" "liabilities"} movements))
        budget-deltas (group-by :currency (deltas-by-account-types #{"budget" "goal"} movements))
        all-delta-currencies (into #{} (flatten [(keys net-worth-deltas) (keys budget-deltas)]))]
    (reduce (fn [report currency]
              (let [net-worth-delta (or (-> net-worth-deltas (get currency) first :value) model.money/ZERO)
                    budget-delta (or (-> budget-deltas (get currency) first :value) model.money/ZERO)]
                (if (= budget-delta net-worth-delta)
                  report
                  (validator/add-error report
                                       (budget-allocation-error currency
                                                                budget-delta
                                                                net-worth-delta)))))
            validation-report
            all-delta-currencies)))

(ds/defn validate :- ::validation-report
  [transaction :- ::spec/new-transaction]
  (-> transaction
      validator/base-validation-report
      validate-zero-sum
      validate-budget-allocations))

(def config
  {:new-transaction {:entry-validator validate}})
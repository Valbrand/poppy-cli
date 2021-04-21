(ns budget.reporter.expenses-by-type
  (:require [budget.logic.account :as logic.account]
            [budget.logic.money :as logic.money]
            [budget.logic.transaction :as logic.transaction]
            [budget.model.money :as model.money]
            [budget.model.account :as model.account]
            [budget.model.date-time :as model.date-time]
            [budget.model.transaction :as model.transaction]
            [budget.reporter.common :as reporter.common :refer [println' indent]]
            [budget.utils :as utils]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::expense-account-name (s/and :account-name
                                     (comp #{"expenses"} logic.account/account-name->account-type)))
(s/def ::expenses-report (s/coll-of ::model.money/money))
(s/def ::monthly-report (s/coll-of (reporter.common/monthly-report-spec ::expenses-report)))
(s/def ::yearly-report (s/coll-of (reporter.common/yearly-report-spec ::expenses-report)))
(s/def ::report (s/map-of ::expense-account-name 
                          (s/coll-of (reporter.common/periodic-report-spec ::expenses-report))))

(ds/defn ^:private transactions->movements-for-expense-accounts
  [transactions :- (s/coll-of ::model.transaction/transaction)]
  (->> transactions
       (map (partial logic.transaction/movements-for-account-types #{"expenses"}))
       flatten))

(defn grouped-by-month-year+account->grouped-by-account+month-year
  [grouped-movements]
  (->> grouped-movements
       (mapcat (fn [[month-year movements-by-account]]
                 (map (fn [[account-name movements]]
                        [account-name [month-year (->> movements
                                                       (map :movement/value)
                                                       logic.money/aggregate-monetary-values)]])
                      movements-by-account)))
       (reduce (fn [result [account-name report]]
                 (update result account-name (fnil conj []) report))
               {})))

(ds/defn report :- ::report
  [transactions-for-account-types :- (s/fspec :args (s/cat :account-types (s/coll-of ::model.account/account-type))
                                              :ret (s/coll-of ::model.transaction/transaction))]
  (let [deltas-by-account+month-year (->> (transactions-for-account-types #{"expenses"})
                                          (group-by (comp model.date-time/month-year :meta/created-at))
                                          (utils/map-vals (comp (partial group-by :movement/account)
                                                                transactions->movements-for-expense-accounts))
                                          grouped-by-month-year+account->grouped-by-account+month-year)
        deltas-by-account+year (->> deltas-by-account+month-year
                                    (utils/map-vals (comp (partial utils/map-vals (comp logic.money/aggregate-monetary-values
                                                                                        flatten
                                                                                        (partial map second)))
                                                          (partial group-by (comp model.date-time/year first)))))]
    (->> (keys deltas-by-account+month-year)
         (map (fn [account-name]
                [account-name (reporter.common/sorted-report (get deltas-by-account+month-year account-name)
                                                             (get deltas-by-account+year account-name))]))
         (into {}))))

(ds/defn present!
  [report :- ::report]
  (println' "Expenses by account:")
  (indent 2
    (doseq [[account-name periodic-reports] report]
      (when (seq periodic-reports)
        (println' (str account-name ":"))
        (reporter.common/present-periodic-reports! periodic-reports reporter.common/print-monetary-values)))))

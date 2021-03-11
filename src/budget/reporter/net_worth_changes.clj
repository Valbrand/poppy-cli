(ns budget.reporter.net-worth-changes
  (:require [budget.logic.account :as logic.account]
            [budget.logic.money :as logic.money]
            [budget.logic.transaction :as logic.transaction]
            [budget.model.account :as model.account]
            [budget.model.date-time :as model.date-time]
            [budget.model.money :as model.money]
            [budget.model.transaction :as model.transaction]
            [budget.reporter.common :refer [indent println' print-monetary-values]]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::net-worth-delta (s/cat :in any?
                                :out (s/coll-of ::model.money/money)
                                :total (s/coll-of ::model.money/money)))
(s/def ::monthly-report (s/tuple ::model.date-time/date ::net-worth-delta))
(s/def ::yearly-report (s/tuple int? ::net-worth-delta))
(s/def ::periodic-report (s/or :yearly-report ::yearly-report
                               :monthly-report ::monthly-report))
(s/def ::report (s/coll-of ::periodic-report))

(def ^:private relevant-account-types #{"assets" "liabilities"})

(ds/defn ^:private transactions->movements-for-relevant-accounts
  [transactions :- (s/coll-of ::model.transaction/transaction)]
  (->> transactions
       (map (partial logic.transaction/movements-for-account-types relevant-account-types))
       flatten))

(ds/defn ^:private internal-transaction? :- boolean?
  "Returns true if it was just a mmoney movement between asset/liabilities accounts."
  [transaction :- ::model.transaction/transaction]
  (->> transaction
       :transaction/movements
       (map (comp logic.account/account-name->account-type :movement/account))
       (into #{})
       (set/intersection #{"incomes" "expenses" "equity"})
       empty?))

(ds/defn ^:private movement-type :- #{:in :out}
  [movement :- ::model.transaction/movement]
  (if (logic.money/negative-value? (:movement/value movement))
    :out
    :in))

(ds/defn ^:private aggregate-in-out-movements :- ::net-worth-delta
  [movements :- (s/coll-of ::model.transaction/movement)]
  (let [{:keys [in out]} (->> movements
                              (group-by movement-type))
        in-aggregated (logic.money/aggregate-monetary-values (map :movement/value in))
        out-aggregated (logic.money/aggregate-monetary-values (map :movement/value out))
        total-aggregated (logic.money/aggregate-monetary-values (flatten [in-aggregated out-aggregated]))]
    [in-aggregated
     out-aggregated
     total-aggregated]))

(ds/defn ^:private monthly-reports->yearly-delta :- ::net-worth-delta
  [monthly-reports :- (s/coll-of ::monthly-report)]
  (->> monthly-reports
       (map second)
       (reduce (fn [[in out total] [in' out' total']]
                 [(into in in')
                  (into out out')
                  (into total total')])
               [[] [] []])
       (map logic.money/aggregate-monetary-values)))

(ds/defn ^:private sorted-report :- ::report
  [monthly-reports :- (s/coll-of ::monthly-report)
   yearly-reports :- (s/coll-of ::yearly-report)]
  (loop [[[month-year :as monthly] & rest-monthly :as monthly-reports] (sort-by first monthly-reports)
         [[year :as yearly] & rest-yearly :as yearly-reports] (sort-by first yearly-reports)
         result []]
    (cond
      (and (nil? monthly)
           (nil? yearly))
      result

      (nil? monthly)
      (recur rest-monthly
             rest-yearly
             (conj result yearly))

      (nil? yearly)
      (recur rest-monthly
             rest-yearly
             (conj result monthly))

      (< year (model.date-time/year month-year))
      (recur monthly-reports
             rest-yearly
             (conj result yearly))
      
      :else
      (recur rest-monthly
             yearly-reports
             (conj result monthly)))))

(ds/defn report :- ::report
  [transactions-for-account-types :- (s/fspec :args (s/cat :account-types (s/coll-of ::model.account/account-type))
                                              :ret (s/coll-of ::model.transaction/transaction))]
  (let [deltas-by-month-year (->> (transactions-for-account-types relevant-account-types)
                                  (remove internal-transaction?)
                                  (group-by (comp model.date-time/month-year :meta/created-at))
                                  (map (juxt first
                                             (comp aggregate-in-out-movements
                                                   transactions->movements-for-relevant-accounts
                                                   second))))
        deltas-by-year (->> deltas-by-month-year
                            (group-by (comp model.date-time/year first))
                            (map (juxt first
                                       (comp monthly-reports->yearly-delta
                                             second))))]
    (sorted-report deltas-by-month-year deltas-by-year)))

(ds/defn ^:private present-net-worth-delta!
  [delta :- ::net-worth-delta]
  (let [{:keys [in out total]} delta]
    (println' "In:")
    (indent 2
      (print-monetary-values in))
    (println' "Out:")
    (indent 2
      (print-monetary-values out))
    (println' "Total:")
    (indent 2
      (print-monetary-values total))))

(ds/defn ^:private present-monthly-report!
  [report :- ::monthly-report]
  (let [[month-year delta] report]
    (println' (model.date-time/month-year-string month-year))
    (indent 2
      (present-net-worth-delta! delta))))

(ds/defn ^:private present-yearly-report!
  [report :- ::yearly-report]
  (let [[year delta] report]
    (println' year)
    (indent 2
      (present-net-worth-delta! delta))))

(ds/defn present!
  [report :- ::report]
  (println' "Net worth changes:")
  
  (indent 2
    (doseq [periodic-report report
            :let [[report-type report-contents] (s/conform ::periodic-report periodic-report)]]
      (case report-type
        :monthly-report
        (present-monthly-report! report-contents)
        
        :yearly-report
        (present-yearly-report! report-contents)))))

(comment
  (sort-by first {1 2 0 3})

  (s/describe ::net-worth-delta)
  (s/conform ::net-worth-delta {:in [{:value 20M, :currency :USD} {:value 21836.41M, :currency :BRL}]
                                :out [{:value -10M, :currency :USD}]
                                :total [{:value 10M, :currency :USD} {:value 21836.41M, :currency :BRL}]})

  (s/conform (s/or :int (s/coll-of (s/cat :a int? :b keyword?)) :str string?)
             [[1 :a]])

  (let [report [2020
                [[{:value 20M, :currency :USD} {:value 21836.41M, :currency :BRL}]
                 [{:value -10M, :currency :USD}]
                 [{:value 10M, :currency :USD} {:value 21836.41M, :currency :BRL}]]]]
    (s/conform ::periodic-report report))

  (let [report [[1 []] [(model.date-time/str->date "2020/02/02") []]]]
    (s/conform ::report report)))
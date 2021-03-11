(ns budget.reporter.account-balances
  (:require [budget.logic.account :as logic.account]
            [budget.logic.money :as logic.money]
            [budget.logic.transaction :as logic.transaction]
            [budget.model.account :as model.account]
            [budget.model.money :as model.money]
            [budget.model.transaction :as model.transaction]
            [budget.reporter.common :refer [indent println' print-monetary-values]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::balances (s/coll-of ::model.money/money))
(s/def ::report (s/map-of :account/name ::balances))

(s/def ::account-types (s/coll-of ::model.account/account-type))
(s/def ::report-title string?)
(s/def ::omit-empty-accounts? boolean?)
(s/def ::report-options (s/keys :req-un [::account-types] :opt-un [::report-title ::omit-empty-accounts?]))

(ds/defn aggregate-movements-value :- (s/coll-of ::model.money/money)
  [movements :- (s/coll-of ::model.transaction/movement)]
  (->> movements
       (map :movement/value)
       logic.money/aggregate-monetary-values))

(ds/defn transactions->balance-total :- (s/coll-of ::model.money/money)
  [account-name :- :account/name
   transactions :- (s/coll-of ::model.transaction/transaction)]
  (->> transactions
       (map (partial logic.transaction/movements-for-account account-name))
       (map aggregate-movements-value)
       flatten
       logic.money/aggregate-monetary-values))

(ds/defn report :- ::report
  [accounts :- (s/coll-of ::model.account/account)
   transactions-for-account :- (s/fspec :args (s/cat :account-name :account/name)
                                        :ret (s/coll-of ::model.transaction/transaction))]
  (->> accounts
       (map (fn [{:account/keys [name]}]
              [name (->> name
                         transactions-for-account
                         (transactions->balance-total name))]))
       (group-by (comp logic.account/account-name->account-type first))
       (map (fn [[account-type reports]]
              [account-type (into {} (sort-by first reports))]))
       (into {})))

(ds/defn present!
  ([report :- ::report]
   (present! {:account-types model.account/valid-account-types} report))
  ([options :- ::report-options
    report :- ::report]
   (let [{:keys [account-types report-title omit-empty-accounts?] :or {report-title         "Account balances"
                                                                       omit-empty-accounts? false}} options
         account-presentation-order (->> account-types
                                         (map-indexed #(vector %2 %1))
                                         (into {}))]
     (letfn [(present-accounts! [accounts]
               (doseq [[account-name balances] (remove #(and omit-empty-accounts?
                                                             (every? logic.money/zero-value? (second %)))
                                                       accounts)]
                 (println' account-name)
                 (indent 2
                   (print-monetary-values balances))))]
       (println' (format "%s:" report-title))
       (indent 2
         (doseq [[account-type accounts] (->> report
                                              (filter #(contains? account-presentation-order (first %)))
                                              (sort-by (comp account-presentation-order first))
                                              (map (juxt first (comp (partial sort-by first) second))))]
           (println' (str/upper-case account-type))
           (indent 2
             (present-accounts! accounts))))))))

(comment
  (present! {"assets" {"assets/nuconta" [{:value 100M, :currency :BRL}]
                       "assets/aaa" [{:value 100M, :currency :BRL}]}
             "equity" {"equity/initial-balance" [{:value -100M, :currency :BRL}]}}))
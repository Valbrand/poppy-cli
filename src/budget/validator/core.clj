(ns budget.validator.core
  (:require [budget.validator.report :as report]
            [budget.validator.spec :as spec]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn base-validation-report :- ::spec/base-validation-report
  [entry :- any?]
  {:errors   []
   :warnings []
   :entry    entry})

(ds/defn add-error :- ::spec/base-validation-report
  [partial-result :- ::spec/base-validation-report
   error :- ::spec/error-or-warning]
  (update partial-result :errors #(conj % error)))

(ds/defn add-warning :- ::spec/base-validation-report
  [partial-result :- ::spec/base-validation-report
   warning :- ::spec/error-or-warning]
  (update partial-result :warnings #(conj % warning)))

(ds/defn ^:private validation-exception
  [report :- ::spec/base-validation-report]
  (ex-info (str/join "\n" (into ["Errors found validating entry:"
                                 (str (:entry report))]
                                (->> (:errors report)
                                     (map report/validation-issue-log-lines)
                                     flatten
                                     (map (partial report/indent 2)))))
           {:details {:report report}}))

(ds/defn validate :- any?
  [validation-config :- ::spec/validation-config-map
   entry-type :- keyword?
   entry :- any?]
  (let [validate (get-in validation-config [entry-type :entry-validator])
        _ (assert (fn? validate)
                  (format "Couldn't find an :entry-validator fn for entry type %s" entry-type))
        {:keys [errors warnings] :as report} (validate entry)]
    (when (seq warnings)
      (report/present-validation-warnings! report))

    (when (seq errors)
      (throw (validation-exception report)))
    
    [entry-type entry]))

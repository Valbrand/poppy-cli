(ns budget.validator.report
  (:require [budget.validator.spec :as spec]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn indent :- string?
  [indent-level :- number? s :- string?]
  (let [leading-spaces (->> \space repeat (take indent-level) str/join)]
    (str leading-spaces s)))

(ds/defn validation-issue-log-lines :- (s/coll-of string?)
  [issue :- ::spec/error-or-warning]
  (into [(format "- %s" (:message issue))
         "Details:"]
        (->> (:details issue)
             (map (comp (partial indent 2) (partial str/join " "))))))

(ds/defn present-validation-warnings!
  [report :- ::spec/base-validation-report]
  (doseq [log-line (into ["Warnings for entry:"
                          (str (:entry report))]
                         (->> (:warnings report)
                              (map validation-issue-log-lines)
                              flatten
                              (map (partial indent 2))))]
    (println log-line)))

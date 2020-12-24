(ns budget.app
  (:require [budget.entries.core.parser :as entries.parser]
            [budget.entries.core.processor :as entries.processor]
            [budget.entries.core.validator :as entries.validator]
            [budget.model.datascript.core :as model.ds]
            [budget.reporter.core :as reporter]
            [budget.state.core :as state]
            [clojure.java.io :as java.io]))

(defn lines->entries
  [lines]
  (->> lines
       entries.parser/parse-entries
       (map entries.validator/validate-entry)))

(defn assimilate-entries!
  [entries state]
  (reduce (fn [state [entry-type entry]]
            (entries.processor/process-entry! state entry-type entry))
          state
          entries))

(defn parse-input-file
  [path]
  (with-open [reader (java.io/reader path)]
    (let [entries (-> reader line-seq lines->entries)
          state (->> (state/new-datascript-state model.ds/schema)
                     entries.processor/initialize-state!
                     (assimilate-entries! entries))
          reports-generator (reporter/reports-graph {:state state})
          default-report-types [:account-balances :current-net-worth :budget-allocation]
          reports (map (juxt identity (partial get reports-generator)) default-report-types)]
      (doseq [[report-type report] reports]
        (reporter/present! report-type report)
        (println "=========="))
      {:state state
       :reports reports})))

(comment
  (do (parse-input-file "input.budget") nil)
  (:reports (parse-input-file "input.budget"))

  (try
    (parse-input-file "input.budget")
    (catch Exception e
      (clojure.stacktrace/print-stack-trace e)))

  (ex-message *e)
  (ex-data *e))
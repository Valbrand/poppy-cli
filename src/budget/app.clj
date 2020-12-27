(ns budget.app
  (:require [budget.entries.core.parser :as entries.parser]
            [budget.entries.core.processor :as entries.processor]
            [budget.entries.core.validator :as entries.validator]
            [budget.model.datascript.core :as model.ds]
            [budget.model.date-time :as model.date-time]
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

(defmacro with-time-elapsed-report
  [entries & body]
  `(let [start-time# (System/currentTimeMillis)
         body-val# (do ~@body)
         end-time# (System/currentTimeMillis)
         time-elapsed# (- end-time# start-time#)]
     (println (format "Processed %d entries in %dms."
                      (count ~entries)
                      time-elapsed#))
     body-val#))

(defn processable-entry?
  [today {:meta/keys [created-at]}]
  (model.date-time/not-after? created-at today))

(defn process-entries!
  [entries process-future?]
  (with-time-elapsed-report entries
    (let [entries (cond->> entries
                    (not process-future?)
                    (filter (comp (partial processable-entry? (model.date-time/today)) second))

                    :always
                    (sort-by (comp :meta/created-at second)))
          state (->> (state/new-datascript-state model.ds/schema)
                     entries.processor/initialize-state!
                     (assimilate-entries! entries))
          reports-generator (reporter/reports-graph {:state state})
          default-report-types [:assets-liabilities-balances :budget-goal-balances :current-net-worth :budget-allocation]
          reports (map (juxt identity (partial get reports-generator)) default-report-types)]
      (doseq [[report-type report] reports]
        (reporter/present! report-type report)
        (println "==========")))))

(defn parse-input-file!
  [path process-future?]
  (with-open [reader (java.io/reader path)]
    (-> reader
        line-seq
        lines->entries
        (process-entries! process-future?))))

(defn run-from-cli!
  [{:keys [file process-future?] :or {process-future? false}}]
  (parse-input-file! (str file) process-future?))

(comment
  (do (run-from-cli! {:file "input.budget" :process-future? true}) nil)
  (:reports (run-from-cli! {:file "input.budget"}))

  (ex-message *e)
  (ex-data *e))
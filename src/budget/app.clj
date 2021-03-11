(ns budget.app
  (:require [budget.entries.core.parser :as entries.parser]
            [budget.entries.core.processor :as entries.processor]
            [budget.entries.core.validator :as entries.validator]
            [budget.model.datascript.core :as model.ds]
            [budget.model.date-time :as model.date-time]
            [budget.reporter.core :as reporter]
            [budget.state.core :as state]
            [clojure.java.io :as java.io]
            [clojure.spec.alpha :as s]
            [net.danielcompton.defn-spec-alpha :as ds]))

(s/def ::file string?)
(s/def ::process-future? boolean?)
(s/def ::report-type #{:assets-liabilities-balances :budget-goal-balances :consolidated-net-worth :budget-allocation :net-worth-changes})
(s/def ::replace-reports (s/coll-of ::report-type :kind sequential?))
(s/def ::extra-reports (s/coll-of ::report-type :kind sequential?))
(s/def ::options
  (s/keys :req-un [::file]
          :opt-un [::process-future? ::replace-reports ::extra-reports]))

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

(ds/defn entries-to-process
  [options :- ::options
   entries]
  (cond->> entries
    (not (:process-future? options))
    (filter (comp (partial processable-entry? (model.date-time/today)) second))

    :always
    (sort-by (juxt (comp :meta/created-at second)
                   entries.processor/entry-processing-order))))

(ds/defn reports-to-present
  [options]
  (let [default-report-types [:assets-liabilities-balances :budget-goal-balances :consolidated-net-worth :budget-allocation]
        {:keys [replace-reports extra-reports]
         :or   {extra-reports []}} options]
    (or replace-reports
        (into default-report-types extra-reports))))

(ds/defn computed-reports
  [reports-generator
   options :- ::options]
  (map (juxt identity (partial get reports-generator))
       (reports-to-present options)))

(ds/defn start-state!
  []
  (entries.processor/initialize-state! (state/new-datascript-state model.ds/schema)))

(ds/defn process-entries!
  [entries
   options :- ::options]
  (with-time-elapsed-report entries
    (let [entries (entries-to-process options entries)
          state (start-state!)
          reports-generator (reporter/reports-graph {:state (assimilate-entries! entries state)})
          reports (computed-reports reports-generator options)]
      (doseq [[report-type report] reports]
        (reporter/present! report-type report)
        (println "==========")))))

(ds/defn parse-input-file!
  [options :- ::options]
  (let [path (-> options :file str)]
    (with-open [reader (java.io/reader path)]
      (-> reader
          line-seq
          lines->entries
          (process-entries! options)))))

(ds/defn run-from-cli!
  [options :- ::options]
  (parse-input-file! options))

(comment
  (do (run-from-cli! {:file "input.budget" :process-future? true}) nil)
  ((juxt (comp keys :state)) (run-from-cli! {:file "input.budget"}))

  (ex-message *e)
  (ex-data *e))
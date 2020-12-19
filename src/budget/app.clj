(ns budget.app
  (:require [budget.entries.core.parser :as entries.parser]
            [budget.entries.core.processor :as entries.processor]
            [budget.entries.core.validator :as entries.validator]
            [budget.model.datascript.core :as model.ds]
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
                     (assimilate-entries! entries))]
      state)))

(comment
  (parse-input-file "input.budget")
  
  (def state *1)
  
  (datascript.core/datoms @(:state/connection state) :eavt)
  
  (class
   (with-open [reader (java.io/reader "input.budget")]
     (lines->entries (line-seq reader))))
  
  (ex-message *e)
  (ex-data *e))
(ns budget.processor.core
  (:require [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn ^:private get-processor!
  [config
   entry-type]
  (let [processor (get-in config [entry-type :entry-processor])]
    (assert (fn? processor)
            (format "Couldn't find an :entry-processor fn for entry type %s" entry-type))
    processor))

(defn process!
  [config
   state
   entry-type
   entry]
  (let [entry-processor (get-processor! config entry-type)]
    (entry-processor state entry)
    state))
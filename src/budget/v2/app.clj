(ns budget.v2.app
  (:require [budget.v2.entries.core.parser :as entries.parser]
            [budget.v2.entries.core.validator :as entries.validator]
            [clojure.java.io :as java.io]))

(defn parse-input-file
  [path]
  (with-open [rdr (java.io/reader path)]
    (->> rdr
         line-seq
         entries.parser/parse-entries
         (map entries.validator/validate-entry)
         (mapv identity))))

(comment
  (parse-input-file "input.budget")
  
  (ex-data *e))
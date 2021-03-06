(ns budget.entries.core.parser
  (:require [budget.parser.core :as parser]
            [budget.entries.core.spec :as entries.spec]
            [budget.entries.new-account.parser :as new-account.parser]
            [budget.entries.new-transaction.parser :as new-transaction.parser]
            [budget.entries.set-exchange-rate.parser :as set-exchange-rate.parser]
            [budget.utils :as utils]
            [clojure.spec.alpha :as s]))

(def entry-parsing-config
  (utils/safe-merge
   new-account.parser/parsing-config
   new-transaction.parser/parsing-config
   set-exchange-rate.parser/parsing-config))

(s/def ::entries.spec/entry (entries.spec/config->spec entry-parsing-config))

(parser/defparser top-level-parser
  (utils/map-vals :header entry-parsing-config))

(def entry-separator? empty?)

(def parse-entry (partial parser/parse-lines
                          top-level-parser
                          entry-parsing-config))

(defn- parse-entries*
  [{:keys [consumed-lines lines] :or {consumed-lines []}}]
  (lazy-seq
   (let [[current-line & rest] lines]
     (cond
       (nil? current-line)
       (some-> consumed-lines
               seq
               parse-entry
               (cons nil))

       (entry-separator? current-line)
       (if (seq consumed-lines)
         (cons (parse-entry consumed-lines)
               (parse-entries* {:lines rest}))
         (parse-entries* {:lines rest}))

       :else
       (parse-entries* {:consumed-lines (conj consumed-lines current-line)
                        :lines          rest})))))


(defn parse-entries
  [lines]
  (parse-entries* {:lines lines}))
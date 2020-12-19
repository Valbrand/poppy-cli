(ns budget.entries.core.processor
  (:require [budget.entries.new-account.processor :as new-account.processor]
            [budget.entries.new-transaction.processor :as new-transaction.processor]
            [budget.utils :as utils]
            [budget.processor.core :as processor]))

(def config
  (utils/safe-merge
   new-account.processor/config
   new-transaction.processor/config))

(def process-entry! (partial processor/process! config))

(def initialize-state! (comp new-transaction.processor/initialize-state!
                             new-account.processor/initialize-state!))
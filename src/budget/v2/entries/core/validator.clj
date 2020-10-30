(ns budget.v2.entries.core.validator
  (:require [budget.v2.entries.new-account.validator :as new-account.validator]
            [budget.v2.entries.new-transaction.validator :as new-transaction.validator]
            [budget.v2.utils :as utils]
            [budget.v2.validator.core :as validator]))

(def validation-config
  (utils/safe-merge
   new-account.validator/config
   new-transaction.validator/config))

(def validate-entry (partial apply validator/validate validation-config))

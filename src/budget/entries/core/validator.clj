(ns budget.entries.core.validator
  (:require [budget.entries.new-account.validator :as new-account.validator]
            [budget.entries.new-transaction.validator :as new-transaction.validator]
            [budget.entries.set-exchange-rate.validator :as set-exchange-rate.validator]
            [budget.utils :as utils]
            [budget.validator.core :as validator]))

(def validation-config
  (utils/safe-merge
   new-account.validator/config
   new-transaction.validator/config
   set-exchange-rate.validator/config))

(def validate-entry (partial apply validator/validate validation-config))

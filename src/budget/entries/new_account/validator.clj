(ns budget.entries.new-account.validator
  (:require [budget.validator.core :as validator]))

(def config
  {:new-account {:entry-validator validator/base-validation-report}})
(ns budget.v2.entries.new-account.validator
  (:require [budget.v2.validator.core :as validator]))

(def config
  {:new-account {:entry-validator validator/base-validation-report}})
(ns budget.entries.set-exchange-rate.validator
  (:require [budget.validator.core :as validator]))

(def config
  {:set-exchange-rates {:entry-validator validator/base-validation-report}})

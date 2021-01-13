(ns budget.entries.set-exchange-rate.spec
  (:require [budget.model.money :as model.money]
            [clojure.spec.alpha :as s]))

(s/def :set-exchange-rates/rates
  (s/map-of ::model.money/currency ::model.money/money))

(s/def ::set-exchange-rates
  (s/keys :req [:set-exchange-rates/rates]))

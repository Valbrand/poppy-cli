(ns budget.entries.new-account.spec
  (:require [budget.entries.core.spec :as entries.spec]
            [budget.model.money :as money]
            [budget.utils :as utils]
            [clojure.spec.alpha :as s]))

(s/def :new-account/name ::entries.spec/account-name)
(s/def :new-account/balance ::money/value)
(s/def :new-account/currency ::entries.spec/currency)

(s/def ::new-account (s/and (s/keys :req [:meta/created-at :new-account/name]
                                    :opt [:new-account/balance :new-account/currency])
                            (partial utils/contains-all-keys-or-none? #{:new-account/balance :new-account/currency})))

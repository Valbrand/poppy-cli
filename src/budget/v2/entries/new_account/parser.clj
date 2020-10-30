(ns budget.v2.entries.new-account.parser
  (:require [budget.v2.parser.core :as parser]
            [budget.v2.entries.new-account.spec :as spec]))

(def header-grammar-rule "date <whitespace> <'new-account'> <whitespace>? <':'> <whitespace> account-name <whitespace> signed-integer <whitespace> currency")
(def header-transformer (parser/rules->map {:date           :meta/created-at
                                            :account-name   :new-account/name
                                            :signed-integer :new-account/balance
                                            :currency       :new-account/currency}))

(defn process-body-line
  [_ _]
  (throw (ex-info "new-account entries do not accept any additional lines" {})))

(def parsing-config
  {:new-account {:header {:rule header-grammar-rule
                          :transformer header-transformer}
                 :body-processor process-body-line
                 :spec ::spec/new-account}})

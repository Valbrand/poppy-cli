(ns budget.entries.new-account.parser
  (:require [budget.parser.core :as parser]
            [budget.entries.new-account.spec :as spec]))

(def header-grammar-rule "date <whitespace> <'new-account'> <whitespace>? <':'> <whitespace> account-name <whitespace> signed-number <whitespace> currency (<whitespace> <comment>)?")
(def header-transformer (parser/rules->map {:date          :meta/created-at
                                            :account-name  :new-account/name
                                            :signed-number :new-account/balance
                                            :currency      :new-account/currency}))

(defn process-body-line
  [_ _]
  (throw (ex-info "new-account entries do not accept any additional lines" {})))

(def parsing-config
  {:new-account {:header {:rule header-grammar-rule
                          :transformer header-transformer}
                 :body-processor process-body-line
                 :spec ::spec/new-account}})

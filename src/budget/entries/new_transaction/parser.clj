(ns budget.entries.new-transaction.parser
  (:require [budget.parser.core :as parser]
            [budget.entries.new-transaction.spec :as spec]))

(def header-grammar-rule "date <whitespace> <'tx'> <whitespace>? <':'> <whitespace> words (<whitespace> tags)?")
(def header-transformer (parser/rules->map {:date  :meta/created-at
                                            :words :meta/description
                                            :tags  :meta/tags}))

(parser/defparser body-parser
  {:movement {:rule        "account-name <whitespace> signed-number <whitespace> currency"
              :transformer (parser/rules->map {:account-name  :new-movement/account
                                               :signed-number :new-movement/amount
                                               :currency      :new-movement/currency})}})

(defn process-body-line
  [partial-entry body-line]
  (let [[body-line-type {:new-movement/keys [account amount currency]}] (body-parser body-line)
        movement #:new-movement {:account account
                                 :value   {:value    amount
                                           :currency currency}}]
    (case body-line-type
      :movement
      (update partial-entry :new-transaction/movements (fnil conj []) movement))))

(def parsing-config
  {:new-transaction {:header         {:rule        header-grammar-rule
                                      :transformer header-transformer}
                     :body-processor process-body-line
                     :spec           ::spec/new-transaction}})
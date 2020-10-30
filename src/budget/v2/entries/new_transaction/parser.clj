(ns budget.v2.entries.new-transaction.parser
  (:require [budget.v2.parser.core :as parser]
            [budget.v2.entries.new-transaction.spec :as spec]))

(def header-grammar-rule "date <whitespace> <'tx'> <whitespace>? <':'> <whitespace> words (<whitespace> tags)?")
(def header-transformer (parser/rules->map {:date  :meta/created-at
                                            :words :meta/description
                                            :tags  :meta/tags}))

(parser/defparser body-parser
  {:movement {:rule        "account-name <whitespace> signed-integer <whitespace> currency"
              :transformer (parser/rules->map {:account-name   :new-movement/account
                                               :signed-integer :new-movement/amount
                                               :currency       :new-movement/currency})}})

(defn process-body-line
  [partial-entry body-line]
  (let [[body-line-type body-data] (body-parser body-line)]
    (case body-line-type
      :movement
      (update partial-entry :new-transaction/movements (fnil conj []) body-data))))

(def parsing-config
  {:new-transaction {:header         {:rule        header-grammar-rule
                                      :transformer header-transformer}
                     :body-processor process-body-line
                     :spec           ::spec/new-transaction}})
(ns budget.entries.set-exchange-rate.parser
  (:require [budget.parser.core :as parser]
            [budget.entries.set-exchange-rate.spec :as spec]))

(def header-grammar-rule "date <whitespace> <'exchange-rate'> (<whitespace> <comment>)?")
(def header-transformer (parser/rules->map {:date :meta/created-at}))

(parser/defparser body-parser
  {:exchange-rate {:rule        "currency <whitespace>? <':'> <whitespace> monetary-value (<whitespace> <comment>)?"
                   :transformer (parser/rules->map {:currency       :exchange-rate/from
                                                    :monetary-value :exchange-rate/to})}})

(defn process-body-line
  [partial-entry body-line]
  (let [[body-line-type {:exchange-rate/keys [from to]}] (body-parser body-line)]
    (case body-line-type
      :exchange-rate
      (update partial-entry :set-exchange-rates/rates (fnil assoc {}) from to))))

(def parsing-config
  {:set-exchange-rates {:header         {:rule        header-grammar-rule
                                         :transformer header-transformer}
                        :body-processor process-body-line
                        :spec           ::spec/set-exchange-rates}})

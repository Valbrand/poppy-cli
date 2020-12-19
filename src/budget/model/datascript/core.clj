(ns budget.model.datascript.core
  (:require [budget.model.datascript.account :as model.ds.account]
            [budget.model.datascript.money :as model.ds.money]
            [budget.model.datascript.transaction :as model.ds.transaction]
            [budget.utils :as utils]))

(def schema (utils/safe-merge model.ds.account/schema
                              model.ds.money/schema
                              model.ds.transaction/schema))
(ns budget.logic.account
  (:require [budget.model.account :as model.account]
            [clojure.string :as str]
            [net.danielcompton.defn-spec-alpha :as ds]))

(ds/defn account-name->account-type :- ::model.account/account-type
  [account-name :- :account/name]
  (-> account-name (str/split #"/") first))

(ds/defn account->account-type :- ::model.account/account-type
  [account :- ::model.account/account]
  (-> account :account/name account-name->account-type))

(ds/defn account-name->identifier :- string?
  [account-name :- :account/name]
  (str/replace account-name #"^.+?/" ""))

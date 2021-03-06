(ns budget.state.protocols
  (:require [clojure.spec.alpha :as s]))

(defprotocol AccountStore
  (account-by-name [this account-name])
  (all-accounts [this])

  (put-account! [this account]))

(defprotocol TransactionStore
  (transactions-by-account-name [this account-name])
  (transactions-by-account-types [this account-types])

  (put-transaction! [this transaction])
  (put-transactions! [this transactions]))

(defprotocol ExchangeRateStore
  (exchange-rates [this])

  (set-exchange-rates! [this exchange-rates]))

(s/def ::state (s/and #(satisfies? AccountStore %)
                      #(satisfies? TransactionStore %)
                      #(satisfies? ExchangeRateStore %)))

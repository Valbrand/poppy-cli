(ns budget.state.protocols
  (:require [clojure.spec.alpha :as s]))

(defprotocol AccountStore
  (account-by-name [this account-name])
  (put-account! [this account]))

(defprotocol TransactionStore
  (transactions-by-account-name [this account-name])
  (put-transaction! [this transaction]))

(s/def ::state (s/and #(satisfies? AccountStore %)
                      #(satisfies? TransactionStore %)))

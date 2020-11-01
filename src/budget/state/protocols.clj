(ns budget.v2.state.protocols)

(defprotocol AccountStore
  (account-by-name [this account-name])
  (put-account [this account]))

(defprotocol TransactionStore
  (transactions-by-account-name [this account-name])
  (put-transaction [this transaction]))

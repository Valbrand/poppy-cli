(ns budget.v2.state.core
  (:require [budget.v2.state.protocols :as state.protocols]
            [budget.v2.model.account :as model.account]
            [budget.v2.model.money :as model.money]
            [budget.v2.model.transaction :as model.transaction]))

(defprotocol IState
  (validate-initialization! [this]))

(defrecord State
  []

  IState
  (validate-initialization!
    [this]
    (when-not (:initialized? this)
      (throw (ex-info "State should be created by calling new-state" {}))))

  state.protocols/AccountStore
  (account-by-name
    [this account-name]
    (validate-initialization! this))
  (put-account
    [this account]
    (validate-initialization! this))

  state.protocols/TransactionStore
  (transactions-by-account-name
    [this account-name]
    (validate-initialization! this))
  (put-transaction
    [this transaction]
    (validate-initialization! this)))

(defn new-state
  ([] (new-state {}))
  ([opts]
   (-> (map->State opts)
       (assoc :initialized? true))))

(comment
  (clojure.spec.alpha/def :foo/bar string?)
  (let [schema (clojure.spec.alpha/keys :req [:foo/bar])]
    (meta schema))
  
  (do
    (require '[datascript.db :as db]
             '[datascript.core :as d])
    (def schema (merge model.account/datascript-schema
                       model.money/datascript-schema
                       model.transaction/datascript-schema))
    (def conn (d/create-conn schema)))
  
  (defn add-transacted-keys
    [x]
    (walk/postwalk (fn [k]
                     (if (map? k)
                       (assoc k ::keys (keys k))
                       k))
                   x))
  
  (defn transact!
    [entities]
    (d/transact! conn
                 (map add-transacted-keys entities)))

  (d/transact! conn [{:account/name "Teste"}
                     {:account/name "Teste2"
                      :account/transactions [{:transaction/movements []}]}])

  (defn tap-class
    [x]
    (printf "%s %s\n" (class x) x)
    x)
  
  (d/pull @conn '[*] [:account/name "Teste2"])
  
  (d/entity @conn 2)
  (meta (d/entity @conn 2))
  (walk/postwalk tap-class (d/entity @conn 2))
  
  (:schema @conn)
  
  (-> (d/entity @conn 2) )
  
  (d/q '{:find  [(pull ?a [*])]
         :in    [$ ?a]}
       @conn
       2)
  
  (defn ds->movement
    [ds-movement]
    (let [{:movement/keys [from to value]} ds-movement]
      {:movement/from  (:account/name from)
       :movement/to    (:account/name to)
       :movement/value value}))
  
  (defn movement->ds
    [movement]
    (let [{:movement/keys [from to value]} movement]
      {:movement/from [:account/name from]
       :movement/to [:account/name to]
       :movement/value value}))

  (defn ds->transaction
    [ds-entity]
    (let [{:transaction/keys [movements]} ds-entity]
      {:transaction/movements (map ds->movement movements)}))
  
  (defn transaction->ds
    [transaction]
    (let [{:transaction/keys [movements]} transaction]
      {:transaction/movements (map movement->ds movements)}))

  (defn ds->account
    [ds-entity]
    (let [{:account/keys [name transactions]} ds-entity]
      {:account/name name
       :account/transactions (map ds->transaction transactions)}))
  
  (defn account->ds
    [account]
    (let [{:account/keys [name transactions]} account]
      {:account/name name
       :account/transactions (map transaction->ds transactions)}))

  
  (defn entity-attrs
    [db ref]
    (->> ref
         (d/q '{:find [?a]
                :in [$ ?e]
                :where [[?e ?a _]]}
              db)
         (map first)))
  
  (defn ref-attr?
    [schema attr]
    (if-let [attr-spec (get schema attr)]
      (= :db.type/ref (:db/valueType attr-spec))
      false))
  
  (defn lookup
    [db ref]
    (let [schema (:schema db)
          entity (d/entity db ref)
          attrs (entity-attrs db ref)]
      (doseq [attr attrs]
        (get entity attr)
        (when (ref-attr? schema attr)
          (assoc )))
      entity))

  (lookup @conn [:account/name "Teste2"])
  
  (d/touch (d/entity @conn 2))
  ;; => {:account/name "Teste2", :account/transactions #{#:db{:id 3}}, :db/id 2}

  
  (d/q '{:find [?a]
         :in [$]
         :where [[1 ?a _]]}
       @conn)
  conn
  (d/transact! conn [(account->ds {:account/name         "teste3"
                      :account/transactions [{:transaction/movements [{:movement/from  "Teste"
                                                                       :movement/to    "teste3"
                                                                       :movement/value {:value    100N
                                                                                        :currency :BRL}}]}]})])

  (meta conn)
  
  (-> *1 ffirst :movement/_from)

  (walk/postwalk tap-class
   (d/pull @conn
          '[* {:account/transactions
               [* {:transaction/movements
                   [*]}]}]
          [:account/name "Teste2"]))

  (d/pull)

  (-> *1 ffirst :account/transactions)

  (let [schema (merge model.account/datascript-schema
                      model.money/datascript-schema
                      model.transaction/datascript-schema)]
    schema)

  (-> (new-state)
      (state.protocols/put-account {})))
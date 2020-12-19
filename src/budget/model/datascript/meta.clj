(ns budget.model.datascript.meta)

(def schema {:meta/description {:db/cardinality :db.cardinality/one
                                :db/doc         "Entity description for humans"}
             :meta/tags        {:db/cardinality :db.cardinality/many
                                :db/doc         "Entity tags to help analysis"}
             :meta/date        {:db/cardinality :db.cardinality/one
                                :db/doc         "Date at which the entity was created"}})

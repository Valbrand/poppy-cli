(ns budget.v2.utils
  (:require [clojure.spec.alpha :as s]))

(defn- safe-merge-error
  [maps & _]
  (throw (ex-info "safe-merge could not be performed: key conflict" {:arguments maps})))

(defn safe-merge
  [& maps]
  (apply merge-with (partial safe-merge-error maps) maps))

(defn map-vals
  [f m]
  (into {}
        (map (juxt first (comp f second)))
        m))

(defn value
  [var-or-value]
  (if (var? var-or-value)
    (var-get var-or-value)
    var-or-value))
(s/fdef value
        :args (s/cat :var-or-val (s/or :var var? :val any?))
        :ret any?)

(defn contains-all-keys-or-none?
  [keys m]
  (apply = (map (partial contains? m) keys)))
(s/fdef contains-all-keys-or-none?
        :args (s/cat :keys (s/coll-of keyword? :min-count 1 :kind set?) :map map?)
        :ret boolean?)

(defmacro functionize-macro
  "Internal API. This is just a helper to be used in `apply-macro`."
  [macro]
  `(fn [& args#] (eval (cons '~macro args#))))

(defmacro apply-macro
  "`apply-macro` is not supposed to be used. This is merely a workaround made to generate entry specs from parsing configs"
  [macro args]
  `(apply (functionize-macro ~macro) ~args))
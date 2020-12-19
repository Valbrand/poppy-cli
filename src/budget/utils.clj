(ns budget.utils
  (:require [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]))

(defn tap-reader
  [x]
  `(let [x-val# ~x]
     (pp/pprint '~x)
     (pp/pprint x-val#)
     x-val#))

(defmacro tap
  [x]
  `(let [x-val# ~x]
     (pp/pprint '~x)
     (pp/pprint x-val#)
     x-val#))

(defn- conflicting-keys
  [& maps]
  (let [map-keys (map (comp set keys) maps)
        all-conflicts (for [i (range 0 (count maps))
                            j (range (inc i) (count maps))]
                        (set/intersection (nth map-keys i) (nth map-keys j)))]
    (apply set/union all-conflicts)))

(defn- safe-merge-error
  [maps & _]
  (throw (ex-info (str "safe-merge could not be performed: key conflicts " (apply conflicting-keys maps))
                  {:arguments maps})))

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

(defn assoc-if-some
  ([m k v]
   (if (some? v)
     (assoc m k v)
     m))
  ([m k v & kvs]
   (assert (even? (count kvs)) "assoc-if-some got an odd number of arguments as the key-value pair list")
   (->> kvs
        (partition-all 2)
        (reduce (fn [m [k v]]
                  (assoc-if-some m k v))
                (assoc-if-some m k v)))))
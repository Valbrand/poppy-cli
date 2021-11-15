(ns budget.lens
  (:require [clojure.core]))

(defn- take-second-arg
  [_ x & args]
  x)

(defn- take-last-arg
  [& args]
  (last args))

(def put take-second-arg)

(defn lens
  [getter setter]
  (fn lens-impl
    ([state]
     (getter state))
    ([state f & args]
     (as-> state *
       (getter *)
       (apply f * args)
       (setter state *)))))

(defn lens*
  [xform-getter xform-setter]
  (letfn [(getter [state]
            (transduce xform-getter take-last-arg nil [state]))
          (setter [state value]
            (transduce (xform-setter value) take-last-arg nil [state]))]
    (fn lens-impl*
      ([]
       {::getter getter
        ::setter setter})
      ([state]
       (getter state))
      ([state f & args]
       (as-> state *
         (getter *)
         (apply f * args)
         (setter state *))))))

(declare null-lens**)

(defn lens**
  [lens-impl-factory]
  (let [concrete-lens-impl (lens-impl-factory null-lens**)]
    ^{::lens-impl-factory lens-impl-factory}
    (fn lens-impl**
      ([state]
       (concrete-lens-impl state))
      ([state f & args]
       (as-> state *
         (concrete-lens-impl *)
         (apply f * args)
         (concrete-lens-impl state *))))))

(def ^:private null-lens** ;; Used as a terminator for a lens composition chain
  (fn null-lens-impl**
    ([state] state)
    ([_ value] value)))

(def id**
  (lens**
   (fn [next]
     (fn id-lens-impl**
       ([state] (next state))
       ([state value] (next state value))))))

(def id (lens identity take-second-arg))

(defn key-path [& keys]
  (lens (fn key-getter [state]
          (get-in state keys))
        (fn key-setter [state value]
          (assoc-in state keys value))))

(defn key-path* [& keys]
  (lens*
   (fn key-path-transducer-getter [reducer]
     (fn key-path-transducer-inner
       ([] (reducer))
       ([result] (reducer result))
       ([result value]
        (reducer result (get-in value keys)))))
   (fn key-path-transducer-setter-outer [value-to-set]
     (fn key-path-transducer-setter [reducer]
       (fn key-path-transducer-inner
         ([] (reducer))
         ([result] (reducer result))
         ([result value]
          (reducer result (assoc-in value keys value-to-set))))))))

(defn key-path** [& keys]
  (lens**
   (fn [next]
     (fn key-path-impl**
       ([state]
        (next (get-in state keys)))
       ([state value]
        (update-in state keys next value))))))

(defn index [n]
  (lens (fn nth-getter [state]
          (nth state n))
        (fn nth-setter [state value]
          (if (vector? state)
            (assoc state n value)
            (loop [result (empty state)
                   [current-state-value & rest :as remaining-state] state
                   idx 0]
              (cond
                (empty? remaining-state)
                result

                :else
                (let [v (if (= n idx) value current-state-value)]
                  (recur (conj result v)
                         rest
                         (inc idx)))))))))

(def elements
  (lens**
   (fn [next]
     (fn elements-path-impl**
       ([state]
        (into (empty state)
              (map next)
              state))
       ([state value]
        (into (empty state)
              (map #(next % value))
              state))))))

(defn- reduce-setter
  [state
   [current-lens & rest-lenses :as lenses]
   value]
  (cond
    (empty? lenses)
    value
    
    :else
    (current-lens state
                  put
                  (reduce-setter (current-lens state)
                                 rest-lenses
                                 value))))

(defn compose
  [& lenses]
  (let [reversed-lenses (reverse lenses)]
    (lens (fn compose-getter [state]
            (reduce (fn [intermediary-state lens]
                      (lens intermediary-state))
                    state
                    reversed-lenses))
          (fn compose-setter [state value]
            (reduce-setter state reversed-lenses value)))))

(defn get-factory
  [lens]
  (::lens-impl-factory (meta lens)))

(defn compose**
  [& lenses]
  (lens**
   (->> lenses
        (map get-factory)
        (apply comp))))

(comment
  *e
  ((nth 1) [1 2 3] + 2)

  (do
    (def foo-lens (compose (key-path :foo) (index 0) (key-path :bar)))

    (foo-lens {:bar [{:foo :bar-then-foo}]
               :foo {:bar :foo-then-bar}}
              name))
  
  (do
    (def test-xf
      (fn outer-test-xf [rf1]
        (fn inner-test-xf
          ([] #tap (rf1))
          ([result] #tap (rf1 result))
          ([result input]
           #tap [:test-xf result input rf1]
           (rf1 result input)))))
    
    (defn my-filter [pred]
      (fn outer-my-filter [rf2]
        (fn inner-my-filter
          ([] #tap (rf2))
          ([result] #tap (rf2 result))
          ([result input]
           #tap [:filter result input rf2]
           (if (pred input)
             (rf2 result input)
             result)))))
    
    (defn plus [& plus-args]
      #tap plus-args
      (apply + plus-args))

    (into [] (comp (my-filter odd?) test-xf) (range 4))))
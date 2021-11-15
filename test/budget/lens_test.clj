(ns budget.lens-test
  (:require [budget.lens :as l]
            [clojure.test :refer :all]
            [matcher-combinators.matchers :as matchers]
            [matcher-combinators.test :refer [match?]]))

(deftest id-lens-test
  (let [lens l/id**
        state :foo]
    (is (match? :foo
                (lens state)))

    (is (match? :new-value
                (lens state l/put :new-value)))))

(deftest key-path-lens-single-key-test
  (let [lens (l/key-path** :foo)
        state {:foo :foo-value
               :bar :bar-value}]
    (is (match? :foo-value
                (lens state)))
    
    (is (match? (matchers/equals {:foo :new-value
                                  :bar :bar-value})
                (lens state l/put :new-value)))))

(deftest key-path-lens-multiple-keys-test
  (let [lens (l/key-path** :foo :bar)
        state {:foo {:bar :foo-bar-value
                     :qux :foo-qux-value}
               :baz :baz-value}]
    (is (match? :foo-bar-value
                (lens state)))

    (is (match? (matchers/equals {:foo {:bar :new-value
                                        :qux :foo-qux-value}
                                  :baz :baz-value})
                (lens state l/put :new-value)))))

(deftest index-lens-test
  (let [lens (l/index 1)
        state [1 2 3]]
    (is (match? 2
                (lens state)))

    (is (match? [1 :new-value 3]
                (lens state l/put :new-value)))))

(deftest elements-lens-test
  (let [lens l/elements
        state [1 2 3]]
    (is (match? [1 2 3]
                (lens state)))

    (is (match? [:new-value :new-value :new-value]
                (lens state l/put :new-value)))))

(deftest compose-test
  (let [lens (l/compose** (l/key-path** :foo-outer)
                          l/elements
                          (l/key-path** :foo-inner))
        state {:foo-outer [{:foo-inner 1}
                           {:foo-inner 2}
                           {:foo-inner 3}]}]
    (is (match? [1 2 3]
                (lens state)))

    (is (match? {:foo-outer [{:foo-inner :new-value}
                             {:foo-inner :new-value}
                             {:foo-inner :new-value}]}
                (lens state l/put :new-value)))))

(comment 
  (clojure.test/run-tests)
  *e)
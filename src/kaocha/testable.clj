(ns kaocha.testable
  (:refer-clojure :exclude [load])
  (:require [clojure.spec.alpha :as s]
            [kaocha.specs :refer [assert-spec]]
            [kaocha.result :as result]))

(def ^:dynamic *fail-fast?*
  "Should testing terminate immediately upon failure or error?"
  nil)

(defn- try-require [n]
  (try
    (require n)
    true
    (catch java.io.FileNotFoundException e
      false)))

(defn- load-type+validate
  "Try to load a testable type, and validate it both to be a valid generic testable, and a valid instance given the type.

  Implementing a new type means creating a namespace based on type's name, e.g.
  `:my.new/testable` should be in `my.new` or `my.new.testable`.

  This file should implement the multimethods `-load` and `-run`, as well as a
  spec for this type of testable."
  [testable]
  (assert-spec :kaocha/testable testable)
  (let [type (::type testable)]
    (if (qualified-keyword? type)
      (when-not (try-require (symbol (str (namespace type) "." (name type))))
        (try-require (symbol (namespace type))))
      (try-require (symbol (name type))))
    (assert-spec type testable)))

(defmulti -load
  "Given a testable, load the specified tests, producing a test-plan."
  ::type)

(defmethod -load :default [testable]
  (throw (ex-info (str "No implementation of "
                       `load
                       " for "
                       (pr-str (:kaocha.testable/type testable)))
                  {:kaocha.error/reason         :kaocha.error/missing-method
                   :kaocha.error/missing-method `load
                   :kaocha/testable             testable})))

(defn load
  "Given a testable, load the specified tests, producing a test-plan.

  Also performs validation, and lazy loading of the testable type's
  implementation."
  [testable]
  (load-type+validate testable)
  (-load testable))

(s/fdef load
  :args (s/cat :testable :kaocha/testable)
  :ret :kaocha.test-plan/testable)

(defmulti -run
  "Given a test-plan, perform the tests, returning the test results."
  ::type)

(defmethod -run :default [testable]
  (throw (ex-info (str "No implementation of "
                       `run
                       " for "
                       (pr-str (:kaocha.testable/type testable)))
                  {:kaocha.error/reason         :kaocha.error/missing-method
                   :kaocha.error/missing-method `run
                   :kaocha/testable             testable})))

(defn run
  "Given a test-plan, perform the tests, returning the test results.

  Also performs validation, and lazy loading of the testable type's
  implementation."
  [testable]
  (load-type+validate testable)
  (-run testable))

(s/fdef run
  :args (s/cat :testable :kaocha.test-plan/testable)
  :ret :kaocha.result/testable)

;; This stack stuff is still untested. The idea is that we "save" the test results
;; as they become available, because when the thread gets interrupted (Ctrl-C), we
;; need them to render a result.
;;
;; It's a bit of a pickle, we have this wonderful, functional API, but in the end
;; keeping track of progress is a very imperative thing to do, so we need to
;; hopscotch around that.

(def ^:dynamic *stack* (atom []))

(defn- assoc-result [parent child]
  (update parent
          :kaocha.test-plan/tests
          (fn [tests]
            (map #(if (= (:kaocha.testable/id %) (:kaocha.testable/id child))
                    child
                    %)
                 tests))))

(defn- unwind-stack [stack]
  (if (>= (count stack) 2)
    (let [child  (last stack)
          stack  (pop stack)
          parent (last stack)
          stack  (pop stack)]
      (conj stack (assoc-result parent child)))
    (pop stack)))

(defn run-testables
  "Run a collection of testables, returning a result collection."
  [testables]
  (loop [result []
         [test & testables] testables]
    (if test
      (do
        (swap! *stack* conj test)
        (let [r (run test)]
          (swap! *stack* unwind-stack)
          (if (and *fail-fast?* (result/failed? r))
            (reduce into [result [r] testables])
            (recur (conj result r) testables))))
      result)))

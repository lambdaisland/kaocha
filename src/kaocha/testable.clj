(ns kaocha.testable
  (:refer-clojure :exclude [load])
  (:require [clojure.spec.alpha :as s]
            [kaocha.specs :refer [assert-spec]]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]
            [clojure.pprint :as pprint]))

(def ^:dynamic *fail-fast?*
  "Should testing terminate immediately upon failure or error?"
  nil)

(defn- try-require [n]
  (try
    (require n)
    true
    (catch java.io.FileNotFoundException e
      false)))

(defn try-load-third-party-lib [type]
  (if (qualified-keyword? type)
    (when-not (try-require (symbol (str (namespace type) "." (name type))))
      (try-require (symbol (namespace type))))
    (try-require (symbol (name type)))))

(defn- load-type+validate
  "Try to load a testable type, and validate it both to be a valid generic testable, and a valid instance given the type.

  Implementing a new type means creating a namespace based on type's name, e.g.
  `:my.new/testable` should be in `my.new` or `my.new.testable`.

  This file should implement the multimethods `-load` and `-run`, as well as a
  spec for this type of testable."
  [testable]
  (assert-spec :kaocha/testable testable)
  (let [type (::type testable)]
    (try-load-third-party-lib type)
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

(def ^:dynamic *current-testable* nil)
(def ^:dynamic *test-plan* nil)

(defmulti -run
  "Given a test-plan, perform the tests, returning the test results."
  (fn [testable test-plan]
    (::type testable)))

(defmethod -run :default [testable test-plan]
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
  [testable test-plan]
  (load-type+validate testable)
  (binding [*current-testable* testable]
    (-run testable test-plan)))

(s/fdef run
        :args (s/cat :testable :kaocha.test-plan/testable
                     :test-plan :kaocha/test-plan)
        :ret :kaocha.result/testable)

(defn load-testables
  "Load a collection of testables, returing a test-plan collection"
  [testables]
  (doall (map #(cond-> % (not (::skip %)) load) testables)))

(defn run-testables
  "Run a collection of testables, returning a result collection."
  [testables test-plan]
  (loop [result []
         [test & testables] testables]
    (if test
      (let [r (if (::skip test)
                test
                (as-> test %
                  (plugin/run-hook :kaocha.hooks/pre-test % test-plan)
                  (run % test-plan)
                  (plugin/run-hook :kaocha.hooks/post-test % test-plan)))]
        (if (and *fail-fast?* (result/failed? r))
          (reduce into [result [r] testables])
          (recur (conj result r) testables)))
      result)))

(defn test-seq [testable]
  (cons testable (mapcat test-seq (or (:kaocha/tests testable)
                                      (:kaocha.test-plan/tests testable)
                                      (:kaocha.result/tests testable)))))

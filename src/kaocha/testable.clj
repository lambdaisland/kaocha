(ns kaocha.testable
  (:require [clojure.spec.alpha :as s]
            [kaocha.specs :refer [assert-spec]]
            [kaocha.result :as result]))

(def ^:dynamic *fail-fast?* nil)

(defn- testable-type [testable]
  (assert-spec :kaocha/testable testable)
  (let [type (:kaocha.testable/type testable)]
    (assert-spec type testable)
    type))


(defmulti load testable-type)

(defmethod load :default [testable]
  (throw (ex-info (str "No implementation of "
                       `load
                       " for "
                       (pr-str (:kaocha.testable/type testable)))
                  {:kaocha.error/reason         :kaocha.error/missing-method
                   :kaocha.error/missing-method `load
                   :kaocha/testable             testable})))

(s/fdef load
        :args (s/cat :testable :kaocha/testable)
        :ret :kaocha.test-plan/testable)


(defmulti run testable-type)

(defmethod run :default [testable]
  (throw (ex-info (str "No implementation of "
                       `run
                       " for "
                       (pr-str (:kaocha.testable/type testable)))
                  {:kaocha.error/reason         :kaocha.error/missing-method
                   :kaocha.error/missing-method `run
                   :kaocha/testable             testable})))

(s/fdef run
        :args (s/cat :testable :kaocha.test-plan/testable)
        :ret :kaocha.result/testable)


(defn run-testables [testables]
  (loop [result []
         [test & testables] testables]
    (if test
      (let [r (run test)]
        (if (and *fail-fast?* (result/failed? r))
          (reduce into [result [r] testables])
          (recur (conj result r) testables)))
      result)))


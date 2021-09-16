(ns kaocha.private-test
  (:require [clojure.test :refer :all]
            [kaocha.testable :as testable]))

(deftest- private-test
  (is true))

(deftest public-test
  (is true))

(deftest test-load
  (let [testable (testable/load {:kaocha.testable/type :kaocha.type/ns
                                 :kaocha.testable/id :kaocha.private-test
                                 :kaocha.testable/desc "kaocha.private-test"
                                 :kaocha.ns/name 'kaocha.private-test})]
    (is (= 3 (count (:kaocha.test-plan/tests testable))))))

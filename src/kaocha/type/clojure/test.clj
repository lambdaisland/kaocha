(ns kaocha.type.clojure.test
  (:require [kaocha.core-ext :refer :all]
            [clojure.spec.alpha :as s]
            [kaocha.type.ns :as type.ns]
            [kaocha.testable :as testable]
            [kaocha.classpath :as classpath]
            [kaocha.load :as load]
            [clojure.java.io :as io]
            [clojure.test :as t]))

(defmethod testable/-load :kaocha.type/clojure.test [testable]
  (assoc (load/load-test-namespaces testable type.ns/->testable)
         ::testable/desc (str (name (::testable/id testable)) " (clojure.test)")))

(defmethod testable/-run :kaocha.type/clojure.test [testable test-plan]
  (t/do-report {:type :begin-test-suite})
  (let [results (testable/run-testables (:kaocha.test-plan/tests testable) test-plan)
        testable (-> testable
                     (dissoc :kaocha.test-plan/tests)
                     (assoc :kaocha.result/tests results))]
    (t/do-report {:type :end-test-suite
                  :kaocha/testable testable})
    testable))

(s/def :kaocha.type/clojure.test (s/keys :req [:kaocha/source-paths
                                               :kaocha/test-paths
                                               :kaocha/ns-patterns]))

(s/def :kaocha/source-paths (s/coll-of string?))
(s/def :kaocha/test-paths (s/coll-of string?))
(s/def :kaocha/ns-patterns (s/coll-of string?))

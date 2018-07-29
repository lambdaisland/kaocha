(ns kaocha.type.suite
  (:require [kaocha.core-ext :refer :all]
            [clojure.spec.alpha :as s]
            [kaocha.type.ns :as type.ns]
            [kaocha.testable :as testable]
            [kaocha.classpath :as classpath]
            [kaocha.load :as load]
            [clojure.tools.namespace.find :as ctn.find]
            [clojure.java.io :as io]
            [clojure.test :as t]))

(defmethod testable/-load :kaocha.type/suite [testable]
  (load/load-test-namespaces testable type.ns/->testable))

(defmethod testable/-run :kaocha.type/suite [testable test-plan]
  (t/do-report {:type :begin-test-suite})
  (let [results (testable/run-testables (:kaocha.test-plan/tests testable) test-plan)
        testable (-> testable
                     (dissoc :kaocha.test-plan/tests)
                     (assoc :kaocha.result/tests results))]
    (t/do-report {:type :end-test-suite
                  :kaocha/testable testable})
    testable))

(s/def :kaocha.type/suite (s/keys :req [:kaocha.suite/source-paths
                                        :kaocha.suite/test-paths
                                        :kaocha.suite/ns-patterns]))

(s/def :kaocha.suite/source-paths (s/coll-of string?))
(s/def :kaocha.suite/test-paths (s/coll-of string?))
(s/def :kaocha.suite/ns-patterns (s/coll-of string?))

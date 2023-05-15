(ns kaocha.type.clojure.test
  (:refer-clojure :exclude [symbol])
  (:require [kaocha.core-ext :refer :all]
            [clojure.spec.alpha :as spec]
            [kaocha.type.ns :as type.ns]
            [kaocha.testable :as testable]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.load :as load]
            [kaocha.specs]
            [kaocha.test-suite :as test-suite]))

(defmethod testable/-load :kaocha.type/clojure.test [testable]
  (-> testable
      (load/load-test-namespaces type.ns/->testable)
      (testable/add-desc "clojure.test")))

(defmethod testable/-run :kaocha.type/clojure.test [testable test-plan]
  (test-suite/run testable test-plan))

(spec/def :kaocha.type/clojure.test (spec/keys :req [:kaocha/source-paths
                                               :kaocha/test-paths
                                               :kaocha/ns-patterns]))

(hierarchy/derive! :kaocha.type/clojure.test :kaocha.testable.type/suite)
(hierarchy/derive! :kaocha.type/ns :kaocha.testable.type/group)
(hierarchy/derive! :kaocha.type/var :kaocha.testable.type/leaf)

(ns kaocha.type.clojure.test
  (:refer-clojure :exclude [symbol])
  (:require [kaocha.core-ext :refer :all]
            [clojure.spec.alpha :as s]
            [kaocha.type.ns :as type.ns]
            [kaocha.testable :as testable]
            [kaocha.classpath :as classpath]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.load :as load]
            [kaocha.test-suite :as test-suite]
            [clojure.java.io :as io]
            [clojure.test :as t]))

(defmethod testable/-load :kaocha.type/clojure.test [testable]
  (-> testable
      (load/load-namespaces :kaocha/test-paths type.ns/->testable)
      (testable/add-desc "clojure.test")))

(defmethod testable/-run :kaocha.type/clojure.test [testable test-plan]
  (test-suite/run testable test-plan))

(s/def :kaocha.type/clojure.test (s/keys :req [:kaocha/source-paths
                                               :kaocha/test-paths
                                               :kaocha/ns-patterns]))

(s/def :kaocha/source-paths (s/coll-of string?))
(s/def :kaocha/test-paths (s/coll-of string?))
(s/def :kaocha/ns-patterns (s/coll-of string?))

(hierarchy/derive! :kaocha.type/clojure.test :kaocha.testable.type/suite)

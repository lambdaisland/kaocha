(ns kaocha.type.suite-test
  (:require [clojure.test :refer :all]
            [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]))

(def test-suite {:kaocha.testable/type      :kaocha.type/suite
                 :kaocha.testable/id        :a
                 :kaocha.suite/source-paths []
                 :kaocha.suite/test-paths   ["fixtures/a-tests"]
                 :kaocha.suite/ns-patterns  [".*"]})

(deftest load-test
  (is (match? {:kaocha.testable/type      :kaocha.type/suite
               :kaocha.testable/id        :a
               :kaocha.suite/source-paths []
               :kaocha.suite/test-paths   ["fixtures/a-tests"]
               :kaocha.suite/ns-patterns  [".*"]
               :kaocha.test-plan/tests    [{:kaocha.testable/type   :kaocha.type/ns
                                            :kaocha.testable/id     :foo.bar-test
                                            :kaocha.ns/name         'foo.bar-test
                                            :kaocha.ns/ns           ns?
                                            :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/var
                                                                      :kaocha.testable/id   :foo.bar-test/a-test
                                                                      :kaocha.var/name      'foo.bar-test/a-test
                                                                      :kaocha.var/var       var?
                                                                      :kaocha.var/test      fn?}]}]}
              (testable/load test-suite))))

(deftest run-test
  (-> test-suite
      testable/load
      testable/run)

  )

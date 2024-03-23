(ns kaocha.fixtures-test
  (:refer-clojure :exclude [symbol])
  (:require [clojure.test :as t :refer [testing is deftest]]
            [kaocha.test-factories :as f]
            [kaocha.testable :as testable]
            [kaocha.classpath :as classpath]
            [kaocha.test-helper]
            [kaocha.core-ext :refer :all]
            [kaocha.test-util :refer [with-test-ctx]]
            [kaocha.type.var]
            [matcher-combinators.test :refer [match?]]))

(deftest once-fixtures-test
  (classpath/add-classpath "fixtures/d-tests")
  (testing "once fixture calling f twice"
    (require 'ddd.double-once-fixture-test)
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? false}
            (testable/run (testable/load {:kaocha.testable/type :kaocha.type/ns
                                          :kaocha.testable/id   :ddd.double-once-fixture-test
                                          :kaocha.testable/desc "ddd.double-once-fixture-test"
                                          :kaocha.ns/name       'ddd.double-once-fixture-test})
                          (f/test-plan {})))]

      (is (match? {:kaocha.testable/type :kaocha.type/ns
                   :kaocha.testable/id   :ddd.double-once-fixture-test
                   :kaocha.testable/desc "ddd.double-once-fixture-test"
                   :kaocha.result/tests
                   [{:kaocha.testable/type :kaocha.type/var
                     :kaocha.testable/id   :ddd.double-once-fixture-test/example-fail-test
                     :kaocha.testable/desc "example-fail-test"
                     :kaocha.var/name      'ddd.double-once-fixture-test/example-fail-test
                     :kaocha.var/var       (resolve 'ddd.double-once-fixture-test/example-fail-test)
                     :kaocha.var/test      fn?
                     :kaocha.result/count  1
                     :kaocha.result/pass   0
                     :kaocha.result/error  0
                     :kaocha.result/fail   1}
                    {:kaocha.testable/type :kaocha.type/var
                     :kaocha.testable/id   :ddd.double-once-fixture-test/example-fail-test
                     :kaocha.testable/desc "example-fail-test"
                     :kaocha.var/name      'ddd.double-once-fixture-test/example-fail-test
                     :kaocha.var/var       (resolve 'ddd.double-once-fixture-test/example-fail-test)
                     :kaocha.var/test      fn?
                     :kaocha.result/count  1
                     :kaocha.result/pass   1
                     :kaocha.result/error  0
                     :kaocha.result/fail   0}]}
                  result))

      (is (match? [{:type :begin-test-ns}
                   {:type :begin-test-var}
                   {:type :fail
                    :expected '(= 1 2)
                    :actual '(not (= 1 2))
                    :message nil}
                   {:type :end-test-var}
                   {:type :begin-test-var}
                   {:type :pass
                    :expected '(= 2 2)
                    :actual (list = 2 2)
                    :message nil}
                   {:type :end-test-var}
                   {:type :end-test-ns}]
                  (mapv #(select-keys % [:type :expected :actual :message]) report))))))

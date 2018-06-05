(ns kaocha.type.ns-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]
            [kaocha.test-helper]))

(defn var-name?
  "Predicate for the name of a var, for use in matchers."
  [v n]
  (and (var? v) (= (:name (meta v)) n)))

(deftest load-test
  (let [ns-name  (doto (gensym "test.ns")
                   create-ns
                   (intern (with-meta 'test-1 {:test :test-1}) nil)
                   (intern (with-meta 'test-2 {:test :test-2}) nil))
        _        (dosync (commute @#'clojure.core/*loaded-libs* conj ns-name))
        testable (testable/load {:kaocha.testable/type :kaocha.type/ns
                                 :kaocha.testable/id   (keyword ns-name)
                                 :kaocha.ns/name       ns-name})]

    (is (match? {:kaocha.testable/type   :kaocha.type/ns
                 :kaocha.ns/name         ns-name
                 :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/var,
                                           :kaocha.testable/id   (keyword (str ns-name) "test-1")
                                           :kaocha.var/name      (symbol (str ns-name) "test-1")
                                           :kaocha.var/var       #(var-name? % 'test-1)
                                           :kaocha.var/test      :test-1}
                                          {:kaocha.testable/type :kaocha.type/var
                                           :kaocha.testable/id   (keyword (str ns-name) "test-2")
                                           :kaocha.var/name      (symbol (str ns-name) "test-2")
                                           :kaocha.var/var       #(var-name? % 'test-2)
                                           :kaocha.var/test      :test-2}]}
                testable))))

(deftest run-test
  (is (match? {:kaocha.testable/type :kaocha.type/ns
               :kaocha.testable/id :test
               :kaocha.ns/name 'kaocha.testable-test
               :kaocha.ns/ns ns?
               :kaocha.result/tests [{:kaocha.testable/type :kaocha.type/var
                                      :kaocha.testable/id :kaocha.testable-test/load--default
                                      :kaocha.var/name 'kaocha.testable-test/load--default
                                      :kaocha.var/var var?
                                      :kaocha.var/test fn?
                                      :kaocha.result/count 1
                                      :kaocha.result/pass 1
                                      :kaocha.result/error 0
                                      :kaocha.result/fail 0}
                                     {:kaocha.testable/type :kaocha.type/var
                                      :kaocha.testable/id :kaocha.testable-test/run--default
                                      :kaocha.var/name 'kaocha.testable-test/run--default
                                      :kaocha.var/var var?
                                      :kaocha.var/test fn?
                                      :kaocha.result/count 1
                                      :kaocha.result/pass 1
                                      :kaocha.result/error 0
                                      :kaocha.result/fail 0}]}

              (binding [testable/*fail-fast?* true]
                (-> #:kaocha.testable{:type :kaocha.type/ns
                                      :id :test
                                      :kaocha.ns/name 'kaocha.testable-test}
                    testable/load
                    testable/run)))))

(ns kaocha.testable.ns-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.testable :as testable]
            [kaocha.testable.ns]
            [kaocha.testable.var]
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


(binding [testable/*fail-fast?* true]
  (testable/run
    (testable/load
     #:kaocha.testable{:type :kaocha.type/ns
                       :id :test
                       :kaocha.ns/name 'kaocha.testable-test})


    ))

(ns kaocha.type.ns-test
  (:refer-clojure :exclude [symbol])
  (:require [clojure.test :as t :refer [is deftest]]
            [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]
            [kaocha.test-helper]
            [kaocha.classpath :as classpath]
            [kaocha.test-util :as util :refer [with-test-ctx]]
            [kaocha.output :as out]))

(defn var-name?
  "Predicate for the name of a var, for use in matchers."
  [v n]
  (and (var? v) (= (:name (meta v)) n)))

(deftest load-test
  (classpath/add-classpath "fixtures/a-tests")

  (let [test-plan (testable/load {:kaocha.testable/type :kaocha.type/ns
                                  :kaocha.testable/id   :foo.bar-test
                                  :kaocha.testable/desc "foo.bar-test"
                                  :kaocha.ns/name       'foo.bar-test})]

    (is (match? {:kaocha.testable/type :kaocha.type/ns
                 :kaocha.testable/id   :foo.bar-test
                 :kaocha.testable/desc "foo.bar-test"
                 :kaocha.ns/name       'foo.bar-test
                 :kaocha.ns/ns         #(= % (the-ns 'foo.bar-test))
                 :kaocha.testable/meta nil
                 :kaocha.test-plan/tests
                 [{:kaocha.testable/type :kaocha.type/var
                   :kaocha.testable/id   :foo.bar-test/a-test
                   :kaocha.var/name      'foo.bar-test/a-test
                   :kaocha.var/var       #(= % (resolve 'foo.bar-test/a-test))
                   :kaocha.var/test      #(true? (%))
                   :kaocha.testable/meta #(= % (meta (resolve 'foo.bar-test/a-test)))}]}
                test-plan)))

  (is (thrown? Exception
               (testable/load {:kaocha.testable/type :kaocha.type/ns
                               :kaocha.testable/id   :foo.unknown-test
                               :kaocha.testable/desc "foo.unknown-test"
                               :kaocha.ns/name       'foo.unknown-test}))))

(deftest run-test
  (classpath/add-classpath "fixtures/a-tests")

  (let [testable (testable/load {:kaocha.testable/type :kaocha.type/ns
                                 :kaocha.testable/id   :foo.bar-test
                                 :kaocha.testable/desc "foo.bar-test"
                                 :kaocha.ns/name       'foo.bar-test})]
    (is (match? {:kaocha.testable/type :kaocha.type/ns
                 :kaocha.testable/id   :foo.bar-test
                 :kaocha.ns/name       'foo.bar-test
                 :kaocha.ns/ns         ns?
                 :kaocha.result/tests  [{:kaocha.testable/type  :kaocha.type/var
                                         :kaocha.testable/id    :foo.bar-test/a-test
                                         :kaocha.testable/desc  "a-test"
                                         :kaocha.var/name       'foo.bar-test/a-test
                                         :kaocha.var/var        var?
                                         :kaocha.var/test       fn?
                                         :kaocha.result/count   1
                                         :kaocha.result/pass    1
                                         :kaocha.result/error   0
                                         :kaocha.result/pending 0
                                         :kaocha.result/fail    0}]}
                (:result
                 (with-test-ctx {:fail-fast? true}
                   (testable/run testable testable)))))))

(require '[kaocha.config :as config])

(deftest run-test-parallel ;both tests currently test the parallel version but later...
  (classpath/add-classpath "fixtures/f-tests")

  (let [testable (testable/load {:kaocha.testable/type    :kaocha.type/clojure.test
                                 :kaocha.testable/id      :unit
                                 :kaocha/ns-patterns      ["-test$"]
                                 :kaocha/source-paths     ["src"]
                                 :kaocha/test-paths       ["fixtures/f-tests"]
                                 :kaocha.filter/skip-meta [:kaocha/skip]})

        #_(testable/load {:kaocha.testable/type :kaocha.type/ns
                                 :kaocha.testable/id   :foo.bar-test
                                 :kaocha.testable/desc "foo.bar-test"
                                 :kaocha.ns/name       'foo.bar-test})]
    (is (match? {:kaocha.testable/type :kaocha.type/ns
                 :kaocha.testable/id   :foo.bar-test
                 :kaocha.ns/name       'foo.bar-test
                 :kaocha.ns/ns         ns?
                 :kaocha.result/tests  [{:kaocha.testable/type  :kaocha.type/var
                                         :kaocha.testable/id    :foo.bar-test/a-test
                                         :kaocha.testable/desc  "a-test"
                                         :kaocha.var/name       'foo.bar-test/a-test
                                         :kaocha.var/var        var?
                                         :kaocha.var/test       fn?
                                         :kaocha.result/count   1
                                         :kaocha.result/pass    1
                                         :kaocha.result/error   0
                                         :kaocha.result/pending 0
                                         :kaocha.result/fail    0}]}
                (:result
                 (with-test-ctx {:fail-fast? true}
                   (testable/run testable testable)))))
    (is (not (nil? (:result
                 (binding [testable/*config* (assoc testable/*config* :parallel true)]
                   (with-test-ctx {:fail-fast? true
                                 :parallel true }
                   (testable/run testable testable)))))))
    ))

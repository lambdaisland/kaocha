(ns kaocha.type.clojure.test-test
  (:refer-clojure :exclude [symbol])
  (:require [clojure.test :refer [is deftest]]
            [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]
            [kaocha.test-util :refer [with-test-ctx]]))

(def test-suite {:kaocha.testable/type :kaocha.type/clojure.test
                 :kaocha.testable/id   :a
                 :kaocha/source-paths  []
                 :kaocha/test-paths    ["fixtures/a-tests"]
                 :kaocha/ns-patterns   [".*"]})

(deftest load-test
  (is (match? {:kaocha.testable/type   :kaocha.type/clojure.test
               :kaocha.testable/id     :a
               :kaocha.testable/desc   "a (clojure.test)"
               :kaocha/source-paths    []
               :kaocha/test-paths      ["fixtures/a-tests"]
               :kaocha/ns-patterns     [".*"]
               :kaocha.test-plan/tests [{:kaocha.testable/type   :kaocha.type/ns
                                         :kaocha.testable/id     :baz.qux-test
                                         :kaocha.ns/name         'baz.qux-test
                                         :kaocha.ns/ns           ns?
                                         :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/var
                                                                   :kaocha.testable/id   :baz.qux-test/nested-test
                                                                   :kaocha.var/name      'baz.qux-test/nested-test
                                                                   :kaocha.var/var       var?
                                                                   :kaocha.var/test      fn?}]}
                                        {:kaocha.testable/type   :kaocha.type/ns
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
  (let [test-plan (testable/load test-suite)]
    (is (match? {:kaocha.testable/type :kaocha.type/clojure.test
                 :kaocha.testable/id   :a
                 :kaocha.testable/desc "a (clojure.test)"
                 :kaocha/source-paths  []
                 :kaocha/test-paths    ["fixtures/a-tests"]
                 :kaocha/ns-patterns   [".*"]
                 :kaocha.result/tests  [{:kaocha.testable/type :kaocha.type/ns
                                         :kaocha.testable/id   :baz.qux-test
                                         :kaocha.ns/name       'baz.qux-test
                                         :kaocha.ns/ns         ns?
                                         :kaocha.result/tests  [{:kaocha.testable/type  :kaocha.type/var
                                                                 :kaocha.testable/id    :baz.qux-test/nested-test
                                                                 :kaocha.var/name       'baz.qux-test/nested-test
                                                                 :kaocha.var/var        var?
                                                                 :kaocha.var/test       fn?
                                                                 :kaocha.result/count   1
                                                                 :kaocha.result/pass    1
                                                                 :kaocha.result/error   1
                                                                 :kaocha.result/fail    0
                                                                 :kaocha.result/pending 0}]}
                                        {:kaocha.testable/type :kaocha.type/ns
                                         :kaocha.testable/id   :foo.bar-test
                                         :kaocha.ns/name       'foo.bar-test
                                         :kaocha.ns/ns         ns?
                                         :kaocha.result/tests  [{:kaocha.testable/type  :kaocha.type/var
                                                                 :kaocha.testable/id    :foo.bar-test/a-test
                                                                 :kaocha.var/name       'foo.bar-test/a-test
                                                                 :kaocha.var/var        var?
                                                                 :kaocha.var/test       fn?
                                                                 :kaocha.result/count   1
                                                                 :kaocha.result/pass    1
                                                                 :kaocha.result/error   0
                                                                 :kaocha.result/fail    0
                                                                 :kaocha.result/pending 0}]}]}

                (:result (with-test-ctx {} (testable/run test-plan test-plan)))))))

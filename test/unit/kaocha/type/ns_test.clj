(ns kaocha.type.ns-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]
            [kaocha.test-helper]
            [kaocha.classpath :as classpath]
            [kaocha.test-util :refer [with-test-ctx]]))

(defn var-name?
  "Predicate for the name of a var, for use in matchers."
  [v n]
  (and (var? v) (= (:name (meta v)) n)))

(deftest load-test
  (classpath/add-classpath "fixtures/a-tests")

  (let [test-plan (testable/load {:kaocha.testable/type :kaocha.type/ns
                                  :kaocha.testable/id   :foo.bar-test
                                  :kaocha.ns/name       'foo.bar-test})]

    (is (= test-plan
           {:kaocha.testable/type :kaocha.type/ns
            :kaocha.testable/id   :foo.bar-test
            :kaocha.ns/name       'foo.bar-test
            :kaocha.ns/ns         (the-ns 'foo.bar-test)
            :kaocha.testable/meta nil
            :kaocha.test-plan/tests
            [{:kaocha.testable/type :kaocha.type/var
              :kaocha.testable/id   :foo.bar-test/a-test
              :kaocha.var/name      'foo.bar-test/a-test
              :kaocha.var/var       (resolve 'foo.bar-test/a-test)
              :kaocha.var/test      (:test (meta (resolve 'foo.bar-test/a-test)))
              :kaocha.testable/meta (meta (resolve 'foo.bar-test/a-test))}]})))

  (is (match? {:kaocha.testable/type        :kaocha.type/ns
               :kaocha.testable/id          :foo.unknown-test
               :kaocha.ns/name              'foo.unknown-test
               :kaocha.test-plan/load-error #(instance? java.io.FileNotFoundException %)}

              (testable/load {:kaocha.testable/type :kaocha.type/ns
                              :kaocha.testable/id   :foo.unknown-test
                              :kaocha.ns/name       'foo.unknown-test}))))

(deftest run-test
  (classpath/add-classpath "fixtures/a-tests")

  (let [testable (testable/load {:kaocha.testable/type :kaocha.type/ns
                                 :kaocha.testable/id   :foo.bar-test
                                 :kaocha.ns/name       'foo.bar-test})]
    (is (match? {:kaocha.testable/type :kaocha.type/ns
                 :kaocha.testable/id   :foo.bar-test
                 :kaocha.ns/name       'foo.bar-test
                 :kaocha.ns/ns         ns?
                 :kaocha.result/tests  [{:kaocha.testable/type :kaocha.type/var
                                         :kaocha.testable/id   :foo.bar-test/a-test
                                         :kaocha.var/name      'foo.bar-test/a-test
                                         :kaocha.var/var       var?
                                         :kaocha.var/test      fn?
                                         :kaocha.result/count  1
                                         :kaocha.result/pass   1
                                         :kaocha.result/error  0
                                         :kaocha.result/fail   0}]}
                (:result
                 (with-test-ctx {:fail-fast? true}
                   (testable/run testable)))))))

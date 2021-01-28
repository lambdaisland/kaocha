(ns kaocha.api-test
  (:require [clojure.test :refer :all]
            [kaocha.api :refer :all]
            [kaocha.test-util :refer [with-out-err]]
            [slingshot.slingshot :refer [try+]]))

(deftest run-test
  (testing "allows API usage"
    (let [config {:kaocha/tests [{:kaocha.testable/id   :unit
                                  :kaocha.testable/desc "unit (clojure.test)"
                                  :kaocha.testable/type :kaocha.type/clojure.test
                                  :kaocha/test-paths    ["fixtures/a-tests"]
                                  :kaocha/source-paths  ["src"]
                                  :kaocha/ns-patterns   ["-test$"]}]}]
      (is (match?
           {:kaocha.result/tests
            [{:kaocha.testable/id   :unit
              :kaocha.testable/type :kaocha.type/clojure.test
              :kaocha/test-paths    ["fixtures/a-tests"]
              :kaocha/source-paths  ["src"]
              :kaocha/ns-patterns   ["-test$"]
              :kaocha.result/tests
              [{:kaocha.testable/type :kaocha.type/ns
                :kaocha.testable/id   :foo.bar-test
                :kaocha.result/tests
                [{:kaocha.testable/type :kaocha.type/var
                  :kaocha.testable/id   :foo.bar-test/a-test
                  :kaocha.testable/desc "a-test"
                  :kaocha.var/name      'foo.bar-test/a-test
                  :kaocha.result/count  1
                  :kaocha.result/pass   1
                  :kaocha.result/error  0
                  :kaocha.result/fail   0}]}]}]}
           (:result (with-out-err (run config))))))))

(deftest no-tests
  (testing "no tests are found!")
  (is (= :caught (try+
                  (run {})
                  (catch :kaocha/early-exit e
                    :caught)))))

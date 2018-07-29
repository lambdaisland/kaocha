(ns kaocha.result-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [kaocha.report :as report]
            [kaocha.test-factories :as f]))

(deftest totals-test
  (is (= #:kaocha.result{:count 5, :pass 3, :error 1, :fail 1}
         (result/totals
          [{:kaocha.testable/id   :api
            :kaocha.testable/type :kaocha.type/suite
            :kaocha.result/tests  [{:kaocha.testable/id   :ddd.bar-test
                                    :kaocha.testable/type :kaocha.type/ns
                                    :kaocha.result/tests  [{:kaocha.testable/id   :ddd.bar-test/test-1
                                                            :kaocha.testable/type :kaocha.type/var
                                                            :kaocha.result/count  1
                                                            :kaocha.result/fail   0
                                                            :kaocha.result/pass   1
                                                            :kaocha.result/error  0}
                                                           {:kaocha.testable/id   :ddd.bar-test/test-2
                                                            :kaocha.testable/type :kaocha.type/var
                                                            :kaocha.result/count  1
                                                            :kaocha.result/fail   0
                                                            :kaocha.result/pass   1
                                                            :kaocha.result/error  0}]}
                                   {:kaocha.testable/id   :ddd.foo-test
                                    :kaocha.testable/type :kaocha.type/ns
                                    :kaocha.result/tests  [{:kaocha.testable/id   :ddd.foo-test/test-3
                                                            :kaocha.testable/type :kaocha.type/var
                                                            :kaocha.result/count  1
                                                            :kaocha.result/fail   0
                                                            :kaocha.result/pass   0
                                                            :kaocha.result/error  1}
                                                           {:kaocha.testable/id   :ddd.foo-test/test-1
                                                            :kaocha.testable/type :kaocha.type/var
                                                            :kaocha.result/count  1
                                                            :kaocha.result/fail   1
                                                            :kaocha.result/pass   0
                                                            :kaocha.result/error  0}
                                                           {:kaocha.testable/id   :ddd.foo-test/test-2
                                                            :kaocha.testable/type :kaocha.type/var
                                                            :kaocha.result/count  1
                                                            :kaocha.result/fail   0
                                                            :kaocha.result/pass   1
                                                            :kaocha.result/error  0}]}]}]))))

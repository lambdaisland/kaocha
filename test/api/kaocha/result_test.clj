(ns kaocha.result-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [kaocha.report :as report]))

(deftest totals-test
  (= #:kaocha.result{:count 5, :pass 3, :error 1, :fail 1}
     (result/totals
      (with-redefs [t/report report/report-counters]
        (-> [{:kaocha.testable/type :kaocha.type/suite,
              :kaocha.testable/id :api,
              :kaocha.suite/source-paths ["src"],
              :kaocha.suite/ns-patterns ["-test$"],
              :kaocha.suite/test-paths ["fixtures/d-tests"]}]
            testable/load-testables
            testable/run-testables)))))

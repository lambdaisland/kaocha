(ns lambdaisland.kaocha.test-test
  (:require [clojure.test :refer :all]
            [lambdaisland.kaocha.test :refer :all]
            [lambdaisland.kaocha.test-util :refer [with-out-err]]))

(deftest run-test
  (testing "allows API usage"
    (let [config {:suites [{:id :unit
                            :test-paths ["fixtures/a-tests"]}]}]
      (is (= {:pass 1, :fail 0, :error 0, :test 1}
             (:result (with-out-err (run config))))))))

(ns kaocha.test-test
  (:require [clojure.test :refer [deftest testing is are]]
            [kaocha.test :refer :all]
            [kaocha.test-util :refer [with-out-err]]))

(deftest run-test
  (testing "allows API usage"
    (let [config {:suites [{:id :unit
                            :test-paths ["fixtures/a-tests"]}]}]
      (is (= {:pass 1, :fail 0, :error 0, :test 1}
             (:result (with-out-err (run config))))))))

(deftest result->report-test
  (is (= (result->report [])
         {:test 0, :pass 0, :fail 0, :error 0}))

  (is (= (result->report [{:type :pass}])
         {:test 0, :pass 1, :fail 0, :error 0}))

  (is (= (result->report [{:type :begin-test-var}
                          {:type :pass}
                          {:type :fail}
                          {:type :pass}
                          {:type :begin-test-ns}
                          {:type :error}])
         {:test 1, :pass 2, :fail 1, :error 1})))

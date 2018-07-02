(ns kaocha.history-test
  (:require [clojure.test :refer :all]
            [kaocha.history :refer :all]))

(deftest clojure-test-summary-test
  (is (= (clojure-test-summary [])
         {:type :summary :test 0, :pass 0, :fail 0, :error 0}))

  (is (= (clojure-test-summary [{:type :pass}])
         {:type :summary :test 0, :pass 1, :fail 0, :error 0}))

  (is (= (clojure-test-summary [{:type :begin-test-var}
                                {:type :pass}
                                {:type :fail}
                                {:type :pass}
                                {:type :begin-test-ns}
                                {:type :error}])
         {:type :summary :test 1, :pass 2, :fail 1, :error 1})))

(ns kaocha.testable-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t :refer :all]
            [kaocha.report :as report]
            [kaocha.testable :as testable]
            [kaocha.test-helper]))

(s/def :kaocha.type/unknown map?)

(deftest load--default
  (is (thrown-ex-data? "No implementation of kaocha.testable/load for :kaocha.type/unknown"
                       {:kaocha.error/reason         :kaocha.error/missing-method,
                        :kaocha.error/missing-method 'kaocha.testable/load,
                        :kaocha/testable             {:kaocha.testable/type :kaocha.type/unknown
                                                      :kaocha.testable/id   :foo}}
                       (testable/load {:kaocha.testable/type :kaocha.type/unknown
                                       :kaocha.testable/id   :foo}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest run--default
  (is (thrown-ex-data?  "No implementation of kaocha.testable/run for :kaocha.type/unknown"
                        {:kaocha.error/reason         :kaocha.error/missing-method,
                         :kaocha.error/missing-method 'kaocha.testable/run,
                         :kaocha/testable             #:kaocha.testable{:type :kaocha.type/unknown
                                                                        :id   :foo}}
                        (testable/run #:kaocha.testable{:type :kaocha.type/unknown
                                                        :id   :foo}))))


(deftest test-seq-test
  (is (= (testable/test-seq
          {:kaocha.testable/id :x/_1
           :kaocha/tests [{:kaocha.testable/id :y/_1}
                          {:kaocha.testable/id :z/_1}]})
         [{:kaocha.testable/id :x/_1,
           :kaocha/tests [#:kaocha.testable{:id :y/_1}
                          #:kaocha.testable{:id :z/_1}]}
          #:kaocha.testable{:id :y/_1}
          #:kaocha.testable{:id :z/_1}])))

#_
(run-tests)

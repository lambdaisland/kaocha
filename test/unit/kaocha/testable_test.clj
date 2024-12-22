(ns kaocha.testable-test
  (:require [clojure.spec.alpha :as spec]
            [clojure.test :as t :refer :all]
            [kaocha.report :as report]
            [kaocha.testable :as testable]
            [kaocha.test-helper]
            [kaocha.test-factories :as f]))

(spec/def :kaocha.type/unknown map?)

(deftest load--default
  (is (thrown-ex-data? "No implementation of kaocha.testable/load for :kaocha.type/unknown"
                       {:kaocha.error/reason         :kaocha.error/missing-method,
                        :kaocha.error/missing-method 'kaocha.testable/load,
                        :kaocha/testable             {:kaocha.testable/type :kaocha.type/unknown
                                                      :kaocha.testable/id   :foo
                                                      :kaocha.testable/desc "foo"}}
                       (testable/load {:kaocha.testable/type :kaocha.type/unknown
                                       :kaocha.testable/id   :foo
                                       :kaocha.testable/desc "foo"}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest run--default
  (is (thrown-ex-data?  "No implementation of kaocha.testable/run for :kaocha.type/unknown"
                        {:kaocha.error/reason         :kaocha.error/missing-method,
                         :kaocha.error/missing-method 'kaocha.testable/run,
                         :kaocha/testable             #:kaocha.testable{:type :kaocha.type/unknown
                                                                        :id   :foo
                                                                        :desc "foo"}}

                        (testable/run {:kaocha.testable/type :kaocha.type/unknown
                                       :kaocha.testable/id   :foo
                                       :kaocha.testable/desc "foo"}
                          (f/test-plan {})))))

(deftest test-seq-test
  (testing "no skipped tests"
    (is (= (testable/test-seq
             {:kaocha.testable/id :x/_1
              :kaocha/tests [{:kaocha.testable/id :y/_1}
                             {:kaocha.testable/id :z/_1}]})
           [{:kaocha.testable/id :x/_1,
             :kaocha/tests [#:kaocha.testable{:id :y/_1}
                            #:kaocha.testable{:id :z/_1}]}
            #:kaocha.testable{:id :y/_1}
            #:kaocha.testable{:id :z/_1}])))
  (testing "top level test-plan/result is ignored"
    (is (= (testable/test-seq
             {:kaocha/tests [{:kaocha.testable/id :y/_1}
                             {:kaocha.testable/id :z/_1}]})
           [#:kaocha.testable{:id :y/_1}
            #:kaocha.testable{:id :z/_1}])))
  (testing "skipped root testable"
    (is (= (testable/test-seq
             {:kaocha.testable/id :x/_1
              :kaocha.testable/skip true
              :kaocha/tests [{:kaocha.testable/id :y/_1}
                             {:kaocha.testable/id :z/_1}]})
           [])))
  (testing "skipped nested testable"
    (is (= (testable/test-seq
             {:kaocha.testable/id :x/_1
              :kaocha/tests [{:kaocha.testable/id :y/_1
                              :kaocha.testable/skip true}
                             {:kaocha.testable/id :z/_1}]})
           [{:kaocha.testable/id :x/_1,
             :kaocha/tests [#:kaocha.testable{:id :y/_1
                                              :skip true}
                            #:kaocha.testable{:id :z/_1}]}
            #:kaocha.testable{:id :z/_1}]))))

(deftest test-seq-with-skipped-test
  (testing "no skipped tests"
    (is (= (testable/test-seq-with-skipped
             {:kaocha.testable/id :x/_1
              :kaocha/tests [{:kaocha.testable/id :y/_1}
                             {:kaocha.testable/id :z/_1}]})
           [{:kaocha.testable/id :x/_1,
             :kaocha/tests [#:kaocha.testable{:id :y/_1}
                            #:kaocha.testable{:id :z/_1}]}
            #:kaocha.testable{:id :y/_1}
            #:kaocha.testable{:id :z/_1}])))
  (testing "top level test-plan/result is ignored"
    (is (= (testable/test-seq-with-skipped
             {:kaocha/tests [{:kaocha.testable/id :y/_1}
                             {:kaocha.testable/id :z/_1}]})
           [#:kaocha.testable{:id :y/_1}
            #:kaocha.testable{:id :z/_1}])))
  (testing "skipped outer testable"
    (is (= (testable/test-seq-with-skipped
             {:kaocha.testable/id :x/_1
              :kaocha.testable/skip true
              :kaocha/tests [{:kaocha.testable/id :y/_1}
                             {:kaocha.testable/id :z/_1}]})
           [{:kaocha.testable/id :x/_1,
             :kaocha.testable/skip true
             :kaocha/tests [#:kaocha.testable{:id :y/_1}
                            #:kaocha.testable{:id :z/_1}]}
            #:kaocha.testable{:id :y/_1}
            #:kaocha.testable{:id :z/_1}])))
  (testing "skipped nested testable"
    (is (= (testable/test-seq-with-skipped
             {:kaocha.testable/id :x/_1
              :kaocha/tests [{:kaocha.testable/id :y/_1
                              :kaocha.testable/skip true}
                             {:kaocha.testable/id :z/_1}]})
           [{:kaocha.testable/id :x/_1,
             :kaocha/tests
             [#:kaocha.testable{:id :y/_1, :skip true}
              #:kaocha.testable{:id :z/_1}]}
            #:kaocha.testable{:id :y/_1, :skip true}
            #:kaocha.testable{:id :z/_1}]))))

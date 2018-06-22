(ns ddd.foo-test
  (:require  [clojure.test :refer :all]))

(deftest test-1
  (is false))

(deftest test-2
  (is true))

(deftest test-3
  (is (throw (Exception. "fail!"))))

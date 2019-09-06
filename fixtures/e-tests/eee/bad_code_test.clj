(ns eee.bad-code-test
  (:require  [clojure.test :refer :all]))

;; (def oops-iam-commented-out "oops")

(deftest test-1
  (is (= "oops" oops-iam-commented-out)))

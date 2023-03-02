(ns kaocha.bb-tests
  (:require [clojure.test :as t :refer [deftest is]]))

(prn :hello)

(deftest smoke-test
  (is (= 1 2)))

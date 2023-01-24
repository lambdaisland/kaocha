(ns kaocha.post-assertion-test
  (:require [clojure.test :refer [deftest is use-fixtures]]))

(defn- post-assertion-fixture
  "Verify that an :each fixture that makes its own assertions doesn't
  break join-fixtures wrapping."
  [f]
  (f)
  (is (= true true)))

(use-fixtures :each #'post-assertion-fixture)

(deftest post-assertion-simple-test
  (is (= 2 (+ 1 1))))

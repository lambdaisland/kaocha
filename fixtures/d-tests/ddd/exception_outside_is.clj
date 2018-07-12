(ns ddd.exception-outside-is
  (:require  [clojure.test :refer :all]))

(deftest exception-outside-is-test
  (throw (Exception. "booo")))

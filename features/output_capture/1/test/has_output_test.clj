(ns has-output-test
  (:require [clojure.test :refer :all]))

(deftest stdout-pass-test
  (println "You peng zi yuan fang lai")
  (is (= :same :same)))

(deftest stdout-fail-test
  (println "Bu yi le hu?")
  (is (= :same :not-same)))

(ns ddd.double-once-fixture-test
  (:require [clojure.test :refer [deftest is use-fixtures]]))

(def ^:dynamic *val* nil)

(defn doubled-fixture [f]
  (binding [*val* :one]
    (f))
  (binding [*val* :two]
    (f)))

(use-fixtures :once doubled-fixture)

(deftest example-fail-test
  (if (= :one *val*)
    (is (= 1 2))
    (is (= 2 2))))

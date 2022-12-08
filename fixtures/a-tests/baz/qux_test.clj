(ns baz.qux-test
  (:require [clojure.test :as t :refer [deftest is use-fixtures]]))

(defn once-fix
  [f]
  (try (f)
       (catch Throwable _)))

(defn each-fix
  [f]
  (try (f)
       (catch Throwable _)))

(use-fixtures :once #'once-fix)
(use-fixtures :each #'each-fix)

(deftest nested-test
  (is (= 1 1))
  (throw (Exception. "fake exception"))
  (is (= 2 1)))

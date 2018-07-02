(ns foo.hello-test
  (:require [clojure.test :as t]))

(t/deftest pass-1
  (t/is true))

(t/deftest pass-2
  (t/is true))

(t/deftest fail-1
  (t/is true)
  (t/is false)
  (t/is true))

(t/deftest pass-3
  (t/is true))

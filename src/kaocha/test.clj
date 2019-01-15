(ns kaocha.test
  "Work in progress.

  This is intended to eventually be a drop-in replacement for clojure.test, with
  some improvements.

  - deftest will kick-off a test run unless one is already in progress, for easy
    in-buffer eval
  "
  (:require [clojure.test :as t]
            [kaocha.testable :as testable]
            [kaocha.api :as api]))

(defmacro deftest [name & body]
  (if kaocha.api/*active?*
    `(clojure.test/deftest ~name ~@body)
    `(do
       (let [var# (clojure.test/deftest ~name ~@body)]
         (or (find-var 'kaocha.repl/run) (require 'kaocha.repl))
         (kaocha.repl/run var#)))))

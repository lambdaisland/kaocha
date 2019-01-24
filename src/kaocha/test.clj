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

(defmacro deftest
  {:doc (:doc (meta #'clojure.test/deftest))
   :arglists (:arglists (meta #'clojure.test/deftest))}
  [name & body]
  (if kaocha.api/*active?*
    `(clojure.test/deftest ~name ~@body)
    `(do
       (let [var# (clojure.test/deftest ~name ~@body)]
         (or (find-ns 'kaocha.repl) (require 'kaocha.repl))
         ((find-var 'kaocha.repl/run) var#)))))

(defmacro is
  {:doc (:doc (meta #'clojure.test/is))
   :arglists (:arglists (meta #'clojure.test/is))}
  [& args]
  `(clojure.test/is ~@args))

(defmacro testing
  {:doc (:doc (meta #'clojure.test/testing))
   :arglists (:arglists (meta #'clojure.test/testing))}
  [& args]
  `(clojure.test/testing ~@args))

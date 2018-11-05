(ns kaocha.type
  "Utilities for writing new test types."
  (:require [clojure.test :as t]
            [kaocha.result :as result]))

(def initial-counters {:test 0, :pass 0, :fail 0, :error 0, :pending 0})

(def ^:dynamic *intermediate-report*)

(defmacro with-report-counters
  {:style/indent [0]}
  [& body]
  `(binding [*intermediate-report* (or (some-> t/*report-counters* deref) ~initial-counters)]
     (binding [t/*report-counters* (ref *intermediate-report*)]
       ~@body)))

(defn report-count []
  (result/diff-test-result *intermediate-report* @t/*report-counters*))

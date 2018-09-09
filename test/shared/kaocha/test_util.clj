(ns kaocha.test-util
  (:require [clojure.test :as t]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [kaocha.report :as report]))

(def ^:dynamic *report-history* nil)

(defmacro with-test-ctx
  "When testing lower level functions, make sure the necessary shared state is set
  up. This also helps to isolate us from the outer test runner state."
  [opts & body]
  `(binding [t/*report-counters* (ref t/*initial-report-counters*)
             t/*testing-vars* (list)
             *report-history* (atom [])]
     (with-redefs [t/report (fn [m#]
                              (swap! *report-history* conj m#)
                              (report/report-counters m#)
                              (when (:fail-fast? ~opts) (report/fail-fast m#)))]
       (let [result# (do ~@body)]
         {:result result#
          :report @*report-history*}))))

(defmacro with-out-err
  "Captures the return value of the expression, as well as anything written on
  stdout or stderr."
  [& body]
  `(let [o# (java.io.StringWriter.)
         e# (java.io.StringWriter.)]
     (binding [*out* o#
               *err* e#]
       (let [r# (do ~@body)]
         {:out (str o#)
          :err (str e#)
          :result r#}))))

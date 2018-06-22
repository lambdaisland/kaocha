(ns kaocha.monkey-patch
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [kaocha.core-ext :refer :all]))

(defn- test-file-and-line [stacktrace test-fn]
  (let [test-class-name (.getName (class test-fn))
        trace-matches?  #(str/starts-with? (.getClassName ^StackTraceElement %) test-class-name)]
    (when-let [s (first (filter trace-matches? stacktrace))]
      (when (.getFileName s)
        {:file (.getFileName s) :line (.getLineNumber s)}))))

(defn- stacktrace-file-and-line [stacktrace]
  (if (seq stacktrace)
    (let [^StackTraceElement s (first stacktrace)]
      {:file (.getFileName s) :line (.getLineNumber s)})
    {:file nil :line nil}))

;; This is an unfortunate hack. clojure.test/is wraps all assertions in a
;; try/catch, but we actually want to use an exception to signal a failure when
;; using --fail-fast, so we can skip the rest of the assertions in the var. This
;; detects our own fail-fast exception, and rethrows it, rather than reporting
;; it as an error.
;; See also the fail-fast reporter
(alter-var-root #'t/do-report
                (fn [_]
                  (fn [m]
                    (let [test-fn    (:kaocha.var/test (:kaocha/testable m))
                          stacktrace (.getStackTrace (if (exception? (:actual m))
                                                       (:actual m)
                                                       (Thread/currentThread)))
                          file-and-line
                          (or (and test-fn (test-file-and-line stacktrace test-fn))
                              (stacktrace-file-and-line (drop-while
                                                         #(let [cl-name (.getClassName ^StackTraceElement %)]
                                                            (or (str/starts-with? cl-name "java.lang.")
                                                                (str/starts-with? cl-name "clojure.test$")
                                                                (str/starts-with? cl-name "clojure.lang.")
                                                                (str/starts-with? cl-name "sun.reflect.")
                                                                (str/starts-with? cl-name "clojure.core")
                                                                (str/starts-with? cl-name "kaocha.monkey_patch$")))
                                                         stacktrace)))]
                      (t/report
                       (case (:type m)
                         :fail     (merge file-and-line m)
                         :mismatch (merge file-and-line m)  ; matcher-combinators
                         :error    (if (-> m :actual ex-data :kaocha/fail-fast)
                                     (do
                                       (throw (:actual m)))
                                     (merge file-and-line m))
                         m))))))

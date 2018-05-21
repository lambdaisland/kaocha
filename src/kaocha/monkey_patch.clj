(ns kaocha.monkey-patch
  (:require [clojure.test :as t]
            [clojure.string :as str]))

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
                    (t/report
                     (case (:type m)
                       :fail
                       (merge (stacktrace-file-and-line (drop-while
                                                         #(let [cl-name (.getClassName ^StackTraceElement %)]
                                                            (or (str/starts-with? cl-name "java.lang.")
                                                                (str/starts-with? cl-name "clojure.test$")
                                                                (str/starts-with? cl-name "kaocha.monkey_patch$")))
                                                         (.getStackTrace (Thread/currentThread)))) m)
                       :error
                       (if (-> m :actual ex-data :kaocha/fail-fast)
                         (throw (:actual m))
                         (merge (stacktrace-file-and-line (.getStackTrace ^Throwable (:actual m))) m))
                       m)))))

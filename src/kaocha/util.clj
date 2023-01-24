(ns kaocha.util
  (:require [kaocha.platform :as platform]))

(defn lib-path
  "Returns the path for a lib"
  ^String [lib ext]
  (str
   (.. (name lib)
       (replace \- \_)
       (replace \. \/))
   "." ext))

(defn ns-file
  "Find the file for a given namespace (symbol), tries to mimic the resolution
  logic of [[clojure.core/load]]."
  [ns-sym]
  (some
   #(.getResource (clojure.lang.RT/baseLoader) (lib-path ns-sym %))
   ["class" "cljc" "clj"]))

(defn compiler-exception-file-and-line
  "Try to get the file and line number from a CompilerException"
  [^Throwable error]
  ;; On Clojure we get a clojure.lang.Compiler$CompilerException, on babashka we
  ;; get a clojure.lang.ExceptionInfo. Both implement the IExceptioninfo
  ;; interface, so we have a uniform way of getting the location info, although
  ;; what Clojure calls `:source` babashka calls `:file`. Calling `ex-data` on
  ;; other exceptions will return `nil`. Note that we can't actually test
  ;; for `(instance? IExceptioninfo)`, since babashka includes the ExceptionInfo
  ;; class, but not the IExceptionInfo interface. Instead we assume that if we
  ;; get the right `ex-data` that this is the exception we're looking for.
  (let [{:keys [type line file source]} (ex-data error)
        file (or source file)]
    (if (and type file line)
      [file line]
      (when-let [error (.getCause error)]
        (recur error)))))

(defn minimal-test-event
  "Return a reduced version of a test event map, so debug output doesn't blow up
  too much, e.g. in case of deeply nested testables in there."
  [m]
  (cond-> (select-keys m [:type
                          :file
                          :line
                          :var
                          :ns
                          :expected
                          :actual
                          :message
                          :kaocha/testable
                          :debug
                          ::printed-expression])
    (:kaocha/testable m)
    (update :kaocha/testable select-keys [:kaocha.testable/id :kaocha.testable/type])))

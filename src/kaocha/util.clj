(ns kaocha.util)

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
  #_(if (instance? clojure.lang.Compiler$CompilerException error)
    [(.-source ^clojure.lang.Compiler$CompilerException error)
     (.-line ^clojure.lang.Compiler$CompilerException error)]
    (when-let [error (.getCause error)]
      (recur error)))
  (when-let [error (.getCause error)]
    (recur error)))

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

(ns kaocha.jit)

(defmacro jit
  "Just in time loading of dependencies."
  [sym]
  `(do
     (locking ~(if (find-var 'clojure.core/requiring-resolve)
                 'clojure.lang.RT/REQUIRE_LOCK
                 (do (binding [*err* *out*]
                       (println "WARNING: kaocha.jit is not thread-safe before Clojure 1.10"))
                     ::lock))
       (require '~(symbol (namespace sym))))
     (find-var '~sym)))

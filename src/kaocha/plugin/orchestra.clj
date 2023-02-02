(ns kaocha.plugin.orchestra
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [kaocha.plugin :refer [defplugin]]
            [orchestra.spec.test :as orchestra]
            [clojure.spec.alpha :as spec]))

(defplugin kaocha.plugin/orchestra
  (post-load [test-plan]
    ;; Instrument specs after all of the test namespaces have been loaded
    (orchestra/instrument)
    test-plan)

  (post-run [result]
    ;; Unstrument specs after tests have run. This isn't so important
    ;; for CLI testing as the process will exit shortly after the post-run
    ;; step, but is helpful for running Kaocha tests from the REPL.
    (orchestra/unstrument)
    result)

  (pre-report [{:keys [type actual] :as event}]
    ;; Render the explain-out string and add it to the clojure.test :error
    ;; event's message, since orchestra no longer adds the explain-str to the
    ;; exception.
    (let [data (and (instance? clojure.lang.ExceptionInfo actual)
                    (ex-data actual))]
      (if (and (= :error type) (:clojure.spec.alpha/problems data))
        (assoc event :kaocha.report/printed-expression
               (str (.getMessage actual) "\n"
                    (with-out-str (spec/explain-out data))))
        event))))

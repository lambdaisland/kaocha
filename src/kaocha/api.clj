(clojure.core/require 'kaocha.version-check)
(ns kaocha.api
  "Programmable test runner interface."
  (:require [clojure.test :as t]
            [kaocha.config :as config]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.history :as history]
            [kaocha.output :as output]
            [kaocha.plugin :as plugin]
            [kaocha.report :as report]
            [kaocha.result :as result]
            [kaocha.util :as util]
            [kaocha.stacktrace :as stacktrace]
            [kaocha.testable :as testable]
            [slingshot.slingshot :refer [try+ throw+]]))

;; Prevent clj-refactor from "cleaning" these from the ns form
(require 'kaocha.monkey-patch)

(def ^:dynamic *active?*
  "Is Kaocha currently active? i.e. loading or runnning tests."
  false)

(defmacro ^:private with-reporter [r & body]
  `(with-redefs [t/report ~r]
     ~@body))

(defmacro ^{:author "Colin Jones"} set-signal-handler!
  [signal f]
  (if (try (Class/forName "sun.misc.Signal")
           (catch Throwable e))
    `(try
       (sun.misc.Signal/handle
        (sun.misc.Signal. ~signal)
        (proxy [sun.misc.SignalHandler] []
          (handle [signal#] (~f signal#))))
       (catch Throwable e#))
    false))

(defmacro ^:private with-shutdown-hook [f & body]
  `(let [runtime#     (java.lang.Runtime/getRuntime)
         on-shutdown# (Thread. ~f)]
     (.addShutdownHook runtime# on-shutdown#)
     (try
       ~@body
       (finally
         (.removeShutdownHook runtime# on-shutdown#)))))

(defn test-plan [config]
  (let [config (plugin/run-hook :kaocha.hooks/pre-load config)
        tests (:kaocha/tests config)]
    (plugin/run-hook
     :kaocha.hooks/post-load
     (-> config
         (dissoc :kaocha/tests)
         (assoc :kaocha.test-plan/tests (testable/load-testables tests))))))

(defn- resolve-reporter [config]
  (try+
   (let [fail-fast? (:kaocha/fail-fast? config)
         reporter   (:kaocha/reporter config)
         reporter   (-> reporter
                        (cond-> (not (vector? reporter)) vector)
                        (conj 'kaocha.report/report-counters
                              'kaocha.history/track
                              'kaocha.report/dispatch-extra-keys)
                        (cond-> fail-fast? (conj 'kaocha.report/fail-fast))
                        config/resolve-reporter)]
     (assoc config :kaocha/reporter (fn [m]
                                      (try
                                        (reporter m)
                                        (catch clojure.lang.ExceptionInfo e
                                          (if (:kaocha/fail-fast (ex-data e))
                                            (throw e)
                                            (do
                                              (output/error "Error in reporter: " (ex-data e) " when processing " (pr-str (util/minimal-test-event m)))
                                              (stacktrace/print-cause-trace e))))
                                        (catch Throwable t
                                          (output/error "Error in reporter: " (.getClass t) " when processing " (pr-str (util/minimal-test-event m)))
                                          (stacktrace/print-cause-trace t))))))

   (catch :kaocha/reporter-not-found {:kaocha/keys [reporter-not-found]}
     (output/error "Failed to resolve reporter var: " reporter-not-found)
     (throw+ {:kaocha/early-exit 254}))))

(defn run [config]
  (let [plugins      (:kaocha/plugins config)
        plugin-chain (plugin/load-all plugins)]
    (plugin/with-plugins plugin-chain
      (let [config     (plugin/run-hook :kaocha.hooks/config config)
            color?     (:kaocha/color? config)
            fail-fast? (:kaocha/fail-fast? config)
            history    (atom [])]
        (binding [*active?*               true
                  testable/*fail-fast?*   fail-fast?
                  testable/*config*       config
                  history/*history*       history
                  output/*colored-output* color?]
          (with-bindings (config/binding-map config)
            (let [config (resolve-reporter config)]
              (let [test-plan (test-plan config)]

                (when-not (some #(or (hierarchy/leaf? %)
                                     (::testable/load-error %))
                                (testable/test-seq test-plan))
                  (if (not (zero? (count (filter ::testable/skip (testable/test-seq-with-skipped test-plan)))))
                    (output/warn (format (str "All %d tests were skipped."
                                              " Check for misspelled settings in your Kaocha test configuration"
                                              " or incorrect focus or skip filters.")
                                         (count (testable/test-seq-with-skipped test-plan))))
                    (output/warn (str "No tests were found. This may be an issue in your Kaocha test configuration."
                                      " To investigate, check the :test-paths and :ns-patterns keys in tests.edn.")))
                  (throw+ {:kaocha/early-exit 0 }))

                (when (find-ns 'matcher-combinators.core)
                  (require 'kaocha.matcher-combinators))

                ;; The load stage adds directories to the classpath, so at this
                ;; point all vars that are being bound need to exist, if not we
                ;; throw.
                (with-bindings (config/binding-map config :throw-errors)
                  (with-reporter (:kaocha/reporter test-plan)
                    (let [on-exit (fn []
                                    (try
                                      ;; Force reset printing to stdout, since we
                                      ;; don't know where in the process we've
                                      ;; been interrupted, output capturing may
                                      ;; still be in effect.
                                      (System/setOut
                                       (java.io.PrintStream.
                                        (java.io.BufferedOutputStream.
                                         (java.io.FileOutputStream. java.io.FileDescriptor/out))))
                                      (binding [history/*history* history]
                                        (t/do-report (history/clojure-test-summary)))
                                      (catch Throwable t
                                        (println "Exception in exit (SIGINT/ShutDown) handler")
                                        (prn t)
                                        (System/exit 1))))]
                      ;; Prefer a signal handler, but accept a shutdown hook
                      (with-shutdown-hook (if (set-signal-handler! "INT" (fn [_]
                                                                           (on-exit)
                                                                           (System/exit 1)))
                                            (fn [])
                                            on-exit)
                        (let [test-plan (plugin/run-hook :kaocha.hooks/pre-run test-plan)]
                          (binding [testable/*test-plan* test-plan]
                            (let [test-plan-tests (:kaocha.test-plan/tests test-plan)
                                  result-tests    (testable/run-testables test-plan-tests test-plan)
                                  result          (plugin/run-hook :kaocha.hooks/post-run
                                                                   (-> test-plan
                                                                       (dissoc :kaocha.test-plan/tests)
                                                                       (assoc :kaocha.result/tests result-tests)))]
                              (assert (= (count test-plan-tests) (count (:kaocha.result/tests result))))
                              (-> result
                                  result/testable-totals
                                  result/totals->clojure-test-summary
                                  t/do-report)
                              result)))))))))))))))

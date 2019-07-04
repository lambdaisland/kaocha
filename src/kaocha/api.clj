(ns kaocha.api
  "Programmable test runner interface."
  (:require [clojure.test :as t]
            [kaocha.config :as config]
            [kaocha.history :as history]
            [kaocha.output :as output]
            [kaocha.plugin :as plugin]
            [kaocha.report :as report]
            [kaocha.result :as result]
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
                                              (output/error "Error in reporter: " (ex-data e) " when processing " (:type m))
                                              (stacktrace/print-cause-trace e))))
                                        (catch Throwable t
                                          (output/error "Error in reporter: " (.getClass t) " when processing " (:type m))
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
                  history/*history*       history
                  output/*colored-output* color?]
          (with-bindings (config/binding-map config)
            (let [config (resolve-reporter config)]
              (let [test-plan (test-plan config)]
                (when (empty? (:kaocha.test-plan/tests test-plan))
                  (output/warn (str "No tests were found, make sure :test-paths and "
                                    ":ns-patterns are configured correctly in tests.edn."))
                  (throw+ {:kaocha/early-exit 0}))

                (when (find-ns 'matcher-combinators.core)
                  (require 'kaocha.matcher-combinators))

                (with-reporter (:kaocha/reporter test-plan)
                  (with-shutdown-hook (fn []
                                        (println "^C")
                                        (binding [history/*history* history]
                                          (t/do-report (history/clojure-test-summary))))
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
                          result)))))))))))))

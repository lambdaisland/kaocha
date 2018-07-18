(ns kaocha.api
  "Programmable test runner interface."
  (:require [clojure.test :as t]
            [kaocha.monkey-patch]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]
            [kaocha.report :as report]
            [kaocha.history :as history]
            [kaocha.config :as config]
            [kaocha.output :as output]))

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
  (let [tests (:kaocha/tests config)]
    (plugin/run-hook
     :kaocha.hooks/post-load
     (-> config
         (dissoc :kaocha/tests)
         (assoc :kaocha.test-plan/tests (testable/load-testables tests))))))

(defn- reporter [config]
  (let [fail-fast? (:kaocha/fail-fast? config)
        reporter   (-> config
                       :kaocha/reporter
                       (cond-> (not (vector? reporter)) vector)
                       (conj 'kaocha.report/report-counters
                             'kaocha.history/track
                             'kaocha.report/dispatch-extra-keys)
                       (cond-> fail-fast? (conj 'kaocha.report/fail-fast)))]
    (config/resolve-reporter reporter)))

(defn run [config]
  (let [plugins      (:kaocha/plugins config)
        plugin-chain (plugin/load-all plugins)]
    (plugin/with-plugins plugin-chain
      (let [config     (plugin/run-hook :kaocha.hooks/config config)
            fail-fast? (:kaocha/fail-fast? config)
            color?     (:kaocha/color? config)
            reporter   (reporter config)
            test-plan  (test-plan config)
            history    (atom [])]
        (binding [testable/*fail-fast?*   fail-fast?
                  history/*history*       history
                  output/*colored-output* color?]
          (with-reporter reporter
            (with-shutdown-hook (fn []
                                  (println "^C")
                                  (binding [history/*history* history]
                                    (t/do-report (history/clojure-test-summary))))
              (let [test-plan       (plugin/run-hook :kaocha.hooks/pre-run test-plan)]
                (binding [testable/*test-plan* test-plan]
                  (let [test-plan-tests (:kaocha.test-plan/tests test-plan)
                        run-testables   (plugin/run-hook :kaocha.hooks/wrap-run testable/run-testables test-plan)
                        result-tests    (run-testables test-plan-tests test-plan)
                        result          (plugin/run-hook :kaocha.hooks/post-run
                                                         (-> test-plan
                                                             (dissoc :kaocha.test-plan/tests)
                                                             (assoc :kaocha.result/tests result-tests)))]
                    (assert (= (count test-plan-tests) (count (:kaocha.result/tests result))))
                    (-> result
                        result/testable-totals
                        result/totals->clojure-test-summary
                        t/do-report)
                    result))))))))))

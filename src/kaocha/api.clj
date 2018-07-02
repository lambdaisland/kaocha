(ns kaocha.api
  "Programmable test runner interface."
  (:require [clojure.test :as t]
            [kaocha.monkey-patch]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]
            [kaocha.report :as report]
            [kaocha.history :as history]
            [kaocha.config2 :as config]))

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

(defn test-plan [config plugin-chain]
  (let [tests (:kaocha/tests config)]
    (plugin/run-hook
     plugin-chain
     :kaocha.hooks/post-load
     (-> config
         (dissoc :kaocha/tests)
         (assoc :kaocha.test-plan/tests (testable/load-testables tests))))))

(defn- reporter [config]
  (let [fail-fast? (:kaocha/fail-fast? config)
        reporter   (-> config
                       :kaocha/reporter
                       (cond-> (not (vector? reporter)) vector
                               fail-fast? (conj 'kaocha.report/fail-fast))
                       (conj 'kaocha.report/report-counters
                             'kaocha.history/track
                             'kaocha.report/dispatch-extra-keys))]
    (config/resolve-reporter reporter)))

(defn run [config]
  (let [plugins      (:kaocha/plugins config)
        plugin-chain (plugin/load-all plugins)
        run-hook     #(plugin/run-hook plugin-chain %2 %1)

        config     (run-hook config :kaocha.hooks/config)
        fail-fast? (:kaocha/fail-fast? config)
        test-plan  (test-plan config plugin-chain)
        reporter   (reporter config)
        history    (atom [])]
    (binding [testable/*fail-fast?* fail-fast?
              history/*history*     history]
      (with-reporter reporter
        (with-shutdown-hook (fn []
                              (println "^C")
                              (binding [history/*history* history]
                                (t/do-report (history/clojure-test-summary))))
          (let [test-plan (run-hook test-plan :pre-run)
                tests     (:kaocha.test-plan/tests test-plan)
                result    (-> test-plan
                              (run-hook :kaocha.hooks/pre-run)
                              (dissoc :kaocha.test-plan/tests)
                              (assoc :kaocha.result/tests (testable/run-testables tests))
                              (run-hook :kaocha.hooks/post-run))]
            (-> result
                result/testable-totals
                result/totals->clojure-test-summary
                t/do-report)
            result))))))

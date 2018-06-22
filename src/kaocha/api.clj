(ns kaocha.api
  "Programmable test runner interface."
  (:require [clojure.test :as t]
            [kaocha.monkey-patch]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]
            [kaocha.report :as report]
            [kaocha.config2 :as config]))

(defmacro ^:private with-reporter [r & body]
  `(with-redefs [t/report ~r]
     ~@body))

(defn run [config]
  (let [plugins      (:kaocha/plugins config)
        plugin-chain (plugin/load-all plugins)
        run-hook     #(plugin/run-hook plugin-chain %2 %1)
        config       (run-hook config :kaocha.hooks/config)
        fail-fast?   (:kaocha/fail-fast? config)
        reporter     (:kaocha/reporter config)
        tests        (:kaocha/tests config)
        test-plan    (-> config
                         (dissoc :kaocha/tests)
                         (assoc :kaocha.test-plan/tests (testable/load-testables tests))
                         (run-hook :kaocha.hooks/post-load))
        reporter     (config/resolve-reporter
                      (if fail-fast?
                        [reporter report/fail-fast]
                        reporter))
        results      (atom [])]
    (binding [testable/*fail-fast?* fail-fast?
              report/*results*      results]
      (with-reporter reporter
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
              t/do-report))))))

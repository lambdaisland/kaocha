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
  (let [fail-fast? (:kaocha/fail-fast? config)
        reporter   (:kaocha/reporter config)
        tests      (:kaocha/tests config)
        test-plan  (testable/load-testables tests)
        reporter   (config/resolve-reporter
                    (if fail-fast?
                      [reporter report/fail-fast]
                      reporter))
        results    (atom [])]
    (binding [testable/*fail-fast?* fail-fast?
              report/*results*      results]
      (with-reporter reporter
        (let [result (testable/run-testables test-plan)]
          (t/do-report (result/totals->clojure-test-summary (result/totals result))))))))

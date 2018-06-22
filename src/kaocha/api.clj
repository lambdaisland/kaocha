(ns kaocha.api
  "Programmable test runner interface."
  (:require [clojure.test :as t]
            [kaocha.monkey-patch]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]
            [kaocha.report :as report]))

(defmacro ^:private with-reporter [r & body]
  `(with-redefs [t/report ~r]
     ~@body))

(defn api [config]
  (let [fail-fast? (:kaocha/fail-fast? config)]
    (binding [testable/*fail-fast?* fail-fast?]
      (let [result (-> config
                       :kaocha/tests
                       testable/load-testables
                       testable/run-testables)]
        (t/do-report (result/totals->clojure-test-summary (result/totals result)))))))

#_
(with-redefs [t/report report/report-counters]
  (-> {:kaocha/fail-fast? true
       :kaocha/tests [{:kaocha.testable/type :kaocha.type/suite,
                       :kaocha.testable/id :api,
                       :kaocha.suite/source-paths ["src"],
                       :kaocha.suite/ns-patterns ["-test$"],
                       :kaocha.suite/test-paths ["fixtures/d-tests"]}]}
      api))


(comment
  (require '[aero.core :refer (read-config)])

  (clojure.spec.alpha/valid?
   :kaocha/config
   (read-config "tests2.edn"))

  (api (read-config "tests2.edn")))

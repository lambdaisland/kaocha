(ns kaocha.plugin.gc-profiling-test
  (:require [clojure.test :refer [deftest is ]]
            [kaocha.test-helper :refer :all]
            [kaocha.testable :as testable]
            [kaocha.plugin :as plugin]
            [kaocha.test-util :refer [with-test-ctx]]))


(def plugin-chain (plugin/register :kaocha.plugin/gc-profiling []))


(def test-suite {:kaocha.testable/type :kaocha.type/clojure.test
                 :kaocha.testable/id   :a
                 :kaocha/source-paths  []
                 :kaocha/test-paths    ["fixtures/a-tests"]
                 :kaocha/ns-patterns   [".*"]})

(deftest gc-profiling-test
  (plugin/with-plugins plugin-chain
    (is 
      (match? {:kaocha.plugin.gc-profiling/memory-profiling? true
                :kaocha.plugin.gc-profiling/show-individual-tests true}
                (plugin/run-hook :kaocha.hooks/config {})))
    (is

      (let [test-plan (testable/load test-suite)
            test-results (->> (testable/run test-plan test-plan)
                              (with-test-ctx {})
                              :result
                              :kaocha.result/tests)]
        (every? :kaocha.plugin.gc-profiling/delta  test-results)))))

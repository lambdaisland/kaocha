(ns kaocha.plugin.gc-profiling-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set]
            [kaocha.test-helper :refer :all]
            [kaocha.testable :as testable]
            [kaocha.plugin :as plugin]
            [kaocha.test-util :refer [with-test-ctx]]
            [kaocha.plugin.gc-profiling :as gc]))


(def plugin-chain (plugin/register :kaocha.plugin/gc-profiling []))


(def test-suite {:kaocha.testable/type :kaocha.type/clojure.test
                 :kaocha.testable/id   :a
                 :kaocha/source-paths  []
                 :kaocha/test-paths    ["fixtures/a-tests"]
                 :kaocha/ns-patterns   [".*"]})

(deftest convert-bytes
  (testing "Basic values"
    (is  
      (= "1.00GB" (gc/convert-bytes (+ 1 1e9))))
    (is 
      (= "1.00MB" (gc/convert-bytes (+ 1 1e6))))
    (is 
      (= "1.00kB" (gc/convert-bytes 1001)))
    (is
      (= "11B" (gc/convert-bytes 11)))
    (is
      (= "0B" (gc/convert-bytes 0)))
    )
  (testing "Negative values"
    (is  
      (= "-1.00GB" (gc/convert-bytes (+ -1 -1e9))))
    (is 
      (= "-1.00MB" (gc/convert-bytes (+ -1 -1e6))))
    (is 
      (= "-1.00kB" (gc/convert-bytes -1001)))
    (is
      (= "-11B" (gc/convert-bytes -11)))))

(deftest gc-profiling-test
  (plugin/with-plugins plugin-chain
    (is 
      (match? {:kaocha.plugin.gc-profiling/gc-profiling? true
                :kaocha.plugin.gc-profiling/gc-profiling-individual false}
                (plugin/run-hook :kaocha.hooks/config {})))
    (let [result ( plugin/run-hook :kaocha.hooks/cli-options [])]
      (is
        (clojure.set/subset? #{"--[no-]gc-profiling"
                                "--[no-]gc-profiling-individual"} 
                             (set (map second result))))) 
    (is

      (let [test-plan (testable/load test-suite)
            test-results (->> (testable/run test-plan test-plan)
                              (with-test-ctx {})
                              :result
                              :kaocha.result/tests)]
        (every? :kaocha.plugin.gc-profiling/delta  test-results)))))

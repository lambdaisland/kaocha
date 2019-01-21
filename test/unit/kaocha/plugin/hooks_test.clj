(ns kaocha.plugin.hooks-test
  (:require [clojure.test :refer :all]
            [kaocha.plugin.hooks :as hooks]
            [kaocha.testable :as testable]
            [kaocha.test-util :as util]
            [kaocha.plugin :as plugin]
            [clojure.test :as t]))

(deftest pre-report-test
  (is (match? {:report [{:type :fail
                         :went-through-hook? true}]}
              (let [test-plan {:kaocha.hooks/pre-report
                               [(fn [m] (assoc m :went-through-hook? true))]}]
                (util/with-test-ctx {}
                  (binding [plugin/*current-chain* [hooks/hooks-hooks]
                            testable/*test-plan* test-plan]
                    (t/do-report {:type :fail})))))))

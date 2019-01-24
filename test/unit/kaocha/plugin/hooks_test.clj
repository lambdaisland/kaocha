(ns kaocha.plugin.hooks-test
  (:require [kaocha.test :refer :all]
            [kaocha.plugin.hooks :as hooks]
            [kaocha.testable :as testable]
            [kaocha.test-util :as util]
            [kaocha.plugin :as plugin]
            [clojure.test :as t]))

(def ^:dynamic *inside-wrap-run?* false)

(deftest wrap-run-test
  (let [run       (fn []
                    (print "inside-wrap-run?:" *inside-wrap-run?*))
        wrap-run  (fn [run]
                    (fn [& args]
                      (binding [*inside-wrap-run?* true]
                        (apply run args))))
        test-plan {:kaocha.hooks/wrap-run [wrap-run]}]
    (binding [plugin/*current-chain* [hooks/hooks-hooks]]
      (let [run' (plugin/run-hook :kaocha.hooks/wrap-run run test-plan)]
        (is (= "inside-wrap-run?: true"
               (with-out-str (run'))))))))

(deftest pre-report-test
  (is (match? {:report [{:type :fail
                         :went-through-hook? true}]}
              (let [test-plan {:kaocha.hooks/pre-report
                               [(fn [m] (assoc m :went-through-hook? true))]}]
                (util/with-test-ctx {}
                  (binding [plugin/*current-chain* [hooks/hooks-hooks]
                            testable/*test-plan* test-plan]
                    (t/do-report {:type :fail})))))))

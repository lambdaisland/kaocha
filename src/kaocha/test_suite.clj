(ns kaocha.test-suite
  (:require [clojure.test :as t]
            [kaocha.testable :as testable]))

(defn deref-recur [testables]
  (cond (future? testables) (deref testables)
        (vector? testables) (doall (mapv deref-recur testables))
        (seq? testables) (deref-recur (into [] (doall testables)))
        (contains? testables :kaocha.test-plan/tests)
        (update testables :kaocha.test-plan/tests deref-recur)
        (contains? testables :kaocha.result/tests)
        (update testables :kaocha.result/tests deref-recur)
        :else testables))

(defn run [testable test-plan]
  (t/do-report {:type :begin-test-suite})
  (let [results (testable/run-testables (:kaocha.test-plan/tests testable) test-plan) 
        testable (-> testable
                     (dissoc :kaocha.test-plan/tests)
                     (assoc :kaocha.result/tests results))]
    (t/do-report {:type :end-test-suite
                  :kaocha/testable testable})
   testable))

(ns kaocha.test-suite
  (:require [clojure.test :as t]
            [kaocha.testable :as testable]))



(defn run [testable test-plan]
  (t/do-report {:type :begin-test-suite})
  (let [results (testable/run-testables (:kaocha.test-plan/tests testable) test-plan) 
        ;; _ (println "Done derefing")
        ;; __ (println (class results))
        ;; __ (println (class (last results)))
        ;; (doall (map #(if (future? %) (deref %) %)
        ;;                  (testable/run-testables (:kaocha.test-plan/tests testable) test-plan)))
        testable (-> testable
                     (dissoc :kaocha.test-plan/tests)
                     (assoc :kaocha.result/tests results))]
    (t/do-report {:type :end-test-suite
                  :kaocha/testable testable})
    (doto testable tap>)
    ))

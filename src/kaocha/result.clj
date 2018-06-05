(ns kaocha.result
  (:require [clojure.spec.alpha :as s]))

(defn diff-test-result [before after]
  {::pass (apply - (map :pass [after before]))
   ::error (apply - (map :error [after before]))
   ::fail (apply - (map :fail [after before]))})


(defn sum [& rs]
  {::count (apply + (map ::count rs))
   ::pass  (apply + (map ::pass rs))
   ::error (apply + (map ::error rs))
   ::fail  (apply + (map ::fail rs))})

(s/fdef sum
        :args (s/cat :testables (s/* ::testable))
        :ret (s/keys :req [::count ::pass ::error ::fail]))


(defn test-totals
  "Recursively sum up the test result numbers."
  [testable]
  (if-let [ts (::tests testable)]
    (apply sum (map test-totals ts))
    testable))

(s/fdef test-totals
        :args (s/cat :testables (s/* ::testable))
        :ret (s/keys :req [::count ::pass ::error ::fail]))

(defn failed? [testable]
  (let [{::keys [error fail]} (test-totals testable)]
    (or (> error 0) (> fail 0))))

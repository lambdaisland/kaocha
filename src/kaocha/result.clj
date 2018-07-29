(ns kaocha.result
  (:require [clojure.spec.alpha :as s]))

(defn diff-test-result [before after]
  {::pass (apply - (map :pass [after before]))
   ::error (apply - (map :error [after before]))
   ::fail (apply - (map :fail [after before]))})

(defn sum [& rs]
  {::count (apply + (map #(::count % 0) rs))
   ::pass  (apply + (map #(::pass % 0) rs))
   ::error (apply + (map #(::error % 0) rs))
   ::fail  (apply + (map #(::fail % 0) rs))})

(s/fdef sum
        :ret (s/keys :req [::count ::pass ::error ::fail]))

(declare testable-totals)

(defn totals [testables]
  (apply sum (map testable-totals testables)))

(defn testable-totals
  "Recursively sum up the test result numbers."
  [testable]
  (if-let [testables (::tests testable)]
    (merge testable (totals testables))
    (merge (sum) testable)))

(s/fdef testable-totals
        :ret (s/keys :req [::count ::pass ::error ::fail]))

(defn failed? [testable]
  (let [{::keys [error fail]} (testable-totals testable)]
    (or (> error 0) (> fail 0))))

(defn failed-one? [{::keys [error fail] :or {error 0 fail 0}}]
  (or (> error 0) (> fail 0)))

(defn totals->clojure-test-summary [totals]
  {:type :summary
   :test (::count totals)
   :pass (::pass totals)
   :fail (::fail totals)
   :error (::error totals)})

(ns kaocha.result
  (:require [clojure.spec.alpha :as s]))

(defn diff-test-result
  "Subtract two clojure.test style summary maps."
  [before after]
  {::pass    (apply - (map :pass [after before]))
   ::error   (apply - (map :error [after before]))
   ::fail    (apply - (map :fail [after before]))
   ::pending (apply - (map :pending [after before]))})

(defn sum
  "Sum up kaocha result maps."
  [& rs]
  {::count   (apply + (map #(::count % 0) rs))
   ::pass    (apply + (map #(::pass % 0) rs))
   ::error   (apply + (map #(::error % 0) rs))
   ::fail    (apply + (map #(::fail % 0) rs))
   ::pending (apply + (map #(::pending % 0) rs))})

(s/def ::result-map (s/keys :req [::count ::pass ::error ::fail ::pending]))

(s/fdef sum
  :args (s/cat :args (s/* ::result-map))
  :ret ::result-map)

(declare testable-totals)

(defn totals
  "Return a map of summed up results for a collection of testables."
  [testables]
  (apply sum (map testable-totals testables)))

(defn ^:no-gen testable-totals
  "Return a map of summed up results for a testable, including descendants."
  [testable]
  (if-let [testables (::tests testable)]
    (merge testable (totals testables))
    (merge (sum) testable)))

(s/fdef testable-totals
  :args (s/cat :testable (s/or :group (s/keys :req [:kaocha.result/tests])
                               :leaf (s/keys :opt [::count ::pass ::error ::fail ::pending])))
  :ret ::result-map)

(defn failed?
  "Did this testable, or one of its children, fail or error?"
  [testable]
  (let [{::keys [error fail]} (testable-totals testable)]
    (or (> error 0) (> fail 0))))

(defn failed-one?
  "Did this testable fail or error, does not recurse."
  [{::keys [error fail] :or {error 0 fail 0}}]
  (or (> error 0) (> fail 0)))

(defn totals->clojure-test-summary
  "Turn a kaocha-style result map into a clojure.test style summary map."
  [totals]
  {:type    :summary
   :test    (::count totals)
   :pass    (::pass totals)
   :fail    (::fail totals)
   :pending (::pending totals)
   :error   (::error totals)})

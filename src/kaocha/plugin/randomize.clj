(ns kaocha.plugin.randomize
  (:require [kaocha.plugin :as plugin])
  (:import [java.util Random]))

(defn rng [seed]
  (let [rng (java.util.Random. seed)]
    (fn [& _] (.nextInt rng))))

(defn straight-sort [test-plan]
  (if-let [tests (:kaocha.test-plan/tests test-plan)]
    (assoc test-plan
           :kaocha.test-plan/tests
           (->> tests
                (sort-by :kaocha.testable/id)
                (map straight-sort)))
    test-plan))

(defn rng-sort [rng test-plan]
  (if-let [tests (:kaocha.test-plan/tests test-plan)]
    (assoc test-plan
           :kaocha.test-plan/tests
           (->> tests
                (sort-by rng)
                (map (partial rng-sort rng))))
    test-plan))

(defmethod plugin/-register :kaocha.plugin/randomize [_ plugins]
  (conj plugins
        {:kaocha.plugin/id :kaocha.plugin/randomize

         :kaocha.hooks/config
         (fn [config]
           (if (::seed config)
             config
             (assoc config ::seed (rand-int Integer/MAX_VALUE))))

         :kaocha.hooks/post-load
         (fn [test-plan]
           (let [rng (rng (::seed test-plan))]
             (->> test-plan
                  straight-sort
                  (rng-sort rng))))}))

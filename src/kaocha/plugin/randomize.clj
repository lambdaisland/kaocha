(ns kaocha.plugin.randomize
  (:require [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.result :as result])
  (:import (java.util Random)))

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
                (map #(assoc % ::sort-key (rng)))
                (sort-by ::sort-key)
                (map (partial rng-sort rng))))
    test-plan))

(defplugin kaocha.plugin/randomize
  (cli-options [opts]
    (conj opts
          [nil  "--[no-]randomize"     "Run test namespaces and vars in random order."]
          [nil  "--seed SEED"          "Provide a seed to determine the random order of tests."
           :parse-fn #(Integer/parseInt %)]))

  (config [config]
    (let [randomize? (get-in config [:kaocha/cli-options :randomize])
          seed       (get-in config [:kaocha/cli-options :seed])
          config     (merge {::randomize? true}
                            config
                            (when (some? randomize?)
                              {::randomize? randomize?}))]
      (if (::randomize? config)
        (merge {::seed (or seed (rand-int Integer/MAX_VALUE))} config)
        config)))

  (post-load [test-plan]
    (if (::randomize? test-plan)
      (let [rng (rng (::seed test-plan))]
        (->> test-plan
             straight-sort
             (rng-sort rng)))
      test-plan))

  (post-run [test-plan]
    (if (and (::randomize? test-plan) (result/failed? test-plan))
      (print "\nRandomized with --seed" (::seed test-plan)))
    test-plan))

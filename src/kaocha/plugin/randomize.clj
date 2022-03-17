(ns kaocha.plugin.randomize
  (:require [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.result :as result])
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

(defmacro or-some
  ([] nil)
  ([x] x)
  ([x & next]
   `(let [or# ~x]
      (if (some? or#) or# (or-some ~@next)))))

(defn get-randomize
  "Returns the randomize? value for the test plan. Metadata takes precedence
  over test-plan."
  [test-plan]
  (or-some
    (get-in test-plan [:kaocha.testable/meta ::randomize?])
    (::randomize? test-plan)))

(defn rng-sort [rng parent-randomize? test-plan]
  (if-let [tests (:kaocha.test-plan/tests test-plan)]
    (assoc test-plan
           :kaocha.test-plan/tests
           (let [randomize? (or-some (get-randomize test-plan) parent-randomize?)]
             (map (partial rng-sort rng randomize?)
                  (if randomize?
                    (->> tests
                         (map #(assoc % ::sort-key (rng)))
                         (sort-by ::sort-key))
                    tests))))
    test-plan))

(defn uses-randomize?
  "Returns true if the test-plan uses ::randomize? at any test level."
  [test-plan]
  (if (get-randomize test-plan)
    true
    (some uses-randomize? (:kaocha.test-plan/tests test-plan))))

(defplugin kaocha.plugin/randomize
  (cli-options [opts]
    (conj opts
          [nil  "--[no-]randomize"     "Run test namespaces and vars in random order."]
          [nil  "--seed SEED"          "Provide a seed to determine the random order of tests."
           :parse-fn #(Integer/parseInt %)]))

  (config [config]
    (let [randomize?    (get-in config [:kaocha/cli-options :randomize])
          no-randomize? (= false randomize?)
          seed          (get-in config [:kaocha/cli-options :seed])
          config        (merge {::randomize? true}
                               config
                               (when (some? randomize?)
                                 {::randomize? randomize?}))]
      (if (not no-randomize?)
        (merge {::seed (or seed (rand-int Integer/MAX_VALUE))} config)
        config)))

  (post-load [test-plan]
    (if-let [seed (::seed test-plan)]
      (let [rng (rng seed)]
        (->> test-plan
             straight-sort
             (rng-sort rng (get-randomize test-plan))))
      test-plan))

  (post-run [test-plan]
    (if (and (::seed test-plan) (uses-randomize? test-plan) (result/failed? test-plan))
      (print "\nRandomized with --seed" (::seed test-plan)))
    test-plan))

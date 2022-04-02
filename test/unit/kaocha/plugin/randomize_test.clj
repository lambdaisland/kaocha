(ns kaocha.plugin.randomize-test
  (:require [clojure.test :refer :all]
            [kaocha.test-helper :refer :all]
            [kaocha.plugin :as plugin]
            [kaocha.testable :as testable]
            [kaocha.plugin.randomize :as randomize]))


(def plugin-chain (plugin/register :kaocha.plugin/randomize []))

(def test-suite {:kaocha.testable/type :kaocha.type/clojure.test
                 :kaocha.testable/id   :c
                 :kaocha.testable/desc "c (clojure.test)"
                 :kaocha/source-paths  []
                 :kaocha/test-paths    ["fixtures/c-tests"]
                 :kaocha/ns-patterns   [".*"]})

(deftest rng-sort-test
  (let [rng (randomize/rng 123)]
    (is (= {}
          (randomize/rng-sort rng true
            {})))
    (is (= {:kaocha.test-plan/tests [{:kaocha.plugin.randomize/sort-key -1188957731}]}
          (randomize/rng-sort rng true
            {:kaocha.test-plan/tests [{}]})))
    (is (= {:kaocha.test-plan/tests [{}]}
          (randomize/rng-sort rng false
            {:kaocha.test-plan/tests [{}]})))
    (is (match? {:kaocha.test-plan/tests [{}]}
          (randomize/rng-sort rng true
            {:kaocha.testable/meta   {:kaocha.plugin.randomize/randomize? false}
             :kaocha.test-plan/tests [{}]})))
    (is (match? {:kaocha.test-plan/tests [{}]}
          (randomize/rng-sort rng true
            {:kaocha.plugin.randomize/randomize? false
             :kaocha.test-plan/tests             [{}]})))))

(deftest randomize-test
  (plugin/with-plugins plugin-chain
    (is (match? {:kaocha.plugin.randomize/randomize? true
                 :kaocha.plugin.randomize/seed       number?}
                (plugin/run-hook :kaocha.hooks/config {})))

    (is (match? {:kaocha.testable/type   :kaocha.type/clojure.test
                 :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/ns
                                           :kaocha.testable/id   :foo.hello-test
                                           :kaocha.testable/desc "foo.hello-test"
                                           :kaocha.test-plan/tests
                                           [{:kaocha.testable/id :foo.hello-test/pass-1}
                                            {:kaocha.testable/id :foo.hello-test/fail-1}
                                            {:kaocha.testable/id :foo.hello-test/pass-3}
                                            {:kaocha.testable/id :foo.hello-test/pass-2}]}]}

                (plugin/run-hook :kaocha.hooks/post-load
                                 (-> test-suite
                                     (assoc :kaocha.plugin.randomize/seed 123
                                            :kaocha.plugin.randomize/randomize? true)
                                     testable/load))))

    (is (match? {:kaocha.testable/type   :kaocha.type/clojure.test
                 :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/ns
                                           :kaocha.testable/id   :foo.hello-test
                                           :kaocha.testable/desc "foo.hello-test"
                                           :kaocha.test-plan/tests
                                           [{:kaocha.testable/id :foo.hello-test/pass-2}
                                            {:kaocha.testable/id :foo.hello-test/pass-3}
                                            {:kaocha.testable/id :foo.hello-test/fail-1}
                                            {:kaocha.testable/id :foo.hello-test/pass-1}]}]}

                (plugin/run-hook :kaocha.hooks/post-load
                                 (-> test-suite
                                     (assoc :kaocha.plugin.randomize/seed 456
                                            :kaocha.plugin.randomize/randomize? true)
                                     testable/load))))))

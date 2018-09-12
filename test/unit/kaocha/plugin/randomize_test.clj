(ns kaocha.plugin.randomize-test
  (:require [clojure.test :refer :all]
            [kaocha.test-helper :refer :all]
            [kaocha.plugin :as plugin]
            [kaocha.testable :as testable]))


(def plugin-chain (plugin/register :kaocha.plugin/randomize []))

(def test-suite {:kaocha.testable/type         :kaocha.type/clojure.test
                 :kaocha.testable/id           :c
                 :kaocha/source-paths    []
                 :kaocha/test-paths      ["fixtures/c-tests"]
                 :kaocha/ns-patterns     [".*"]})

(deftest randomize-test
  (plugin/with-plugins plugin-chain
    (is (match? {:kaocha.plugin.randomize/randomize? true
                 :kaocha.plugin.randomize/seed       number?}
                (plugin/run-hook :kaocha.hooks/config {})))

    (is (match? {:kaocha.testable/type   :kaocha.type/clojure.test
                 :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/ns
                                           :kaocha.testable/id   :foo.hello-test
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

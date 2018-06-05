(ns kaocha.plugin.randomize-test
  (:require [clojure.test :refer :all]
            [kaocha.test-helper :refer :all]
            [kaocha.plugin :as plugin]
            [kaocha.testable :as testable]))


(def plugin-chain (plugin/register :kaocha.plugin/randomize []))

(def test-suite {:kaocha.testable/type         :kaocha.type/suite
                 :kaocha.testable/id           :c
                 :kaocha.suite/source-paths    []
                 :kaocha.suite/test-paths      ["fixtures/c-tests"]
                 :kaocha.suite/ns-patterns     [".*"]})

(deftest randomize-test
  (is (match? {:kaocha.plugin.randomize/seed number?}
              (plugin/run-step plugin-chain :kaocha.hooks/config {})))

  (is (match? {:kaocha.testable/type   :kaocha.type/suite
               :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/ns
                                         :kaocha.testable/id   :foo.hello-test
                                         :kaocha.test-plan/tests
                                         [{:kaocha.testable/id :foo.hello-test/pass-2}
                                          {:kaocha.testable/id :foo.hello-test/pass-3}
                                          {:kaocha.testable/id :foo.hello-test/pass-1}
                                          {:kaocha.testable/id :foo.hello-test/fail-1}]}]}

              (plugin/run-step plugin-chain :kaocha.hooks/post-load
                               (-> test-suite
                                   (assoc :kaocha.plugin.randomize/seed 123)
                                   testable/load))))

  (is (match? {:kaocha.testable/type   :kaocha.type/suite
               :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/ns
                                         :kaocha.testable/id   :foo.hello-test
                                         :kaocha.test-plan/tests
                                         [{:kaocha.testable/id :foo.hello-test/pass-1}
                                          {:kaocha.testable/id :foo.hello-test/pass-2}
                                          {:kaocha.testable/id :foo.hello-test/pass-3}
                                          {:kaocha.testable/id :foo.hello-test/fail-1}]}]}

              (plugin/run-step plugin-chain :kaocha.hooks/post-load
                               (-> test-suite
                                   (assoc :kaocha.plugin.randomize/seed 456)
                                   testable/load)))))

(ns kaocha.plugin.alpha.spec-test-check-test
  (:require [clojure.spec.test.alpha]
            [clojure.test :refer [deftest testing is]]
            [clojure.tools.cli :as cli]
            [kaocha.plugin :as plugin]
            [matcher-combinators.test]
            [kaocha.plugin.alpha.spec-test-check :as spec-test-check]))

(alias 'stc 'clojure.spec.test.check)

(def suite @#'spec-test-check/default-stc-suite)

(defn run-plugin-hook
  [hook init & extra-args]
  (let [chain (plugin/load-all [:kaocha.plugin.alpha/spec-test-check])]
    (apply plugin/run-hook* chain hook init extra-args)))

(deftest cli-options-test
  (let [cli-opts (run-plugin-hook :kaocha.hooks/cli-options [])]
    (testing "--[no-]stc-instrumentation"
      (is (= {:stc-instrumentation true}
             (:options (cli/parse-opts ["--stc-instrumentation"] cli-opts))))
      (is (= {:stc-instrumentation false}
             (:options (cli/parse-opts ["--no-stc-instrumentation"] cli-opts)))))
    (testing "--[no-]stc-asserts"
      (is (= {:stc-asserts true}
             (:options (cli/parse-opts ["--stc-asserts"] cli-opts))))
      (is (= {:stc-asserts false}
             (:options (cli/parse-opts ["--no-stc-asserts"] cli-opts)))))
    (testing "--stc-num-tests"
      (is (= {:stc-num-tests 5}
             (:options (cli/parse-opts ["--stc-num-tests" "5"]
                                       cli-opts)))))
    (testing "--stc-max-size"
      (is (= {:stc-max-size 10}
             (:options (cli/parse-opts ["--stc-max-size" "10"]
                                       cli-opts)))))))

(deftest config-test
  (is (= {:kaocha/tests [suite]}
         (run-plugin-hook :kaocha.hooks/config {})))

  (testing "::stc/instrument?"
    (is (= {:kaocha/tests     [(assoc suite ::stc/instrument? true)]
            ::stc/instrument? true}
           (run-plugin-hook :kaocha.hooks/config {::stc/instrument? true})))
    (is (= {:kaocha/tests     [(assoc suite ::stc/instrument? false)]
            ::stc/instrument? false}
           (run-plugin-hook :kaocha.hooks/config {::stc/instrument? false})))
    (is (= {:kaocha/tests       [(assoc suite ::stc/instrument? true)]
            ::stc/instrument?   true
            :kaocha/cli-options {:stc-instrumentation true}}
           (run-plugin-hook :kaocha.hooks/config
                            {:kaocha/cli-options {:stc-instrumentation true}})))
    (is (= {:kaocha/tests       [(assoc suite ::stc/instrument? false)]
            ::stc/instrument?   false
            :kaocha/cli-options {:stc-instrumentation false}}
           (run-plugin-hook
            :kaocha.hooks/config
            {:kaocha/cli-options {:stc-instrumentation false}}))))

  (testing "::stc/check-asserts?"
    (is (= {:kaocha/tests        [(assoc suite ::stc/check-asserts? true)]
            ::stc/check-asserts? true}
           (run-plugin-hook :kaocha.hooks/config {::stc/check-asserts? true})))
    (is (= {:kaocha/tests        [(assoc suite ::stc/check-asserts? false)]
            ::stc/check-asserts? false}
           (run-plugin-hook :kaocha.hooks/config {::stc/check-asserts? false})))
    (is (= {:kaocha/tests        [(assoc suite ::stc/check-asserts? true)]
            ::stc/check-asserts? true
            :kaocha/cli-options  {:stc-asserts true}}
           (run-plugin-hook :kaocha.hooks/config
                            {:kaocha/cli-options {:stc-asserts true}})))
    (is (= {:kaocha/tests        [(assoc suite ::stc/check-asserts? false)]
            ::stc/check-asserts? false
            :kaocha/cli-options  {:stc-asserts false}}
           (run-plugin-hook :kaocha.hooks/config
                            {:kaocha/cli-options {:stc-asserts false}}))))

  (testing "::stc/opts"
    (is (= {:kaocha/tests       [(assoc suite ::stc/opts {:num-tests 5})]
            ::stc/opts          {:num-tests 5}
            :kaocha/cli-options {:stc-num-tests 5}}
           (run-plugin-hook :kaocha.hooks/config
                            {:kaocha/cli-options {:stc-num-tests 5}})))
    (is (= {:kaocha/tests       [(assoc suite ::stc/opts {:max-size 5})]
            ::stc/opts          {:max-size 5}
            :kaocha/cli-options {:stc-max-size 5}}
           (run-plugin-hook :kaocha.hooks/config
                            {:kaocha/cli-options {:stc-max-size 5}}))))

  (testing "with existing test suites"
    (is (= {:kaocha/tests        [{:kaocha.testable/type :kaocha.type/spec.test.check
                                   :kaocha.testable/id   :my-special-fdefs
                                   ::stc/instrument?     true
                                   ::stc/check-asserts?  false
                                   ::stc/opts            {:num-tests 5
                                                          :max-size  5}}
                                  {:kaocha.testable/type :kaocha.type/spec.test.check
                                   :kaocha.testable/id   :my-ok-fdefs
                                   ::stc/instrument?     true
                                   ::stc/check-asserts?  true
                                   ::stc/opts            {:num-tests 5
                                                          :max-size  5}}
                                  {:kaocha.testable/id :unit}]
            ::stc/instrument?    true
            ::stc/check-asserts? true
            ::stc/opts           {:num-tests 5
                                  :max-size  5}
            :kaocha/cli-options  {:stc-instrumentation true
                                  :stc-num-tests       5}}
      (run-plugin-hook
       :kaocha.hooks/config
       {:kaocha/tests        [{:kaocha.testable/type :kaocha.type/spec.test.check
                               :kaocha.testable/id   :my-special-fdefs
                               ::stc/check-asserts?  false
                               ::stc/instrument?     true
                               ::stc/opts            {:num-tests 1000}}
                              {:kaocha.testable/type :kaocha.type/spec.test.check
                               :kaocha.testable/id   :my-ok-fdefs}
                              {:kaocha.testable/id :unit}]
        ::stc/check-asserts? true
        ::stc/opts           {:num-tests 10
                              :max-size  5}
        :kaocha/cli-options  {:stc-instrumentation true
                              :stc-num-tests       5}})))))

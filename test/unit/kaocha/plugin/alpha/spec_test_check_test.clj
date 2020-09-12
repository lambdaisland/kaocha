(ns kaocha.plugin.alpha.spec-test-check-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.tools.cli :as cli]
            [kaocha.plugin :as plugin]))

(alias 'stc 'clojure.spec.test.check)

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
  (testing "::stc/instrument?"
    (is (match? {::stc/instrument? nil}
                (run-plugin-hook :kaocha.hooks/config {})))
    (is (match? {::stc/instrument? true}
                (run-plugin-hook :kaocha.hooks/config {::stc/instrument? true})))
    (is (match? {::stc/instrument? false}
                (run-plugin-hook :kaocha.hooks/config
                                 {::stc/instrument? false})))
    (is (match? {::stc/instrument? nil}
                (run-plugin-hook :kaocha.hooks/config {:kaocha/cli-options {}})))
    (is (match? {::stc/instrument? true}
                (run-plugin-hook :kaocha.hooks/config
                                 {:kaocha/cli-options {:stc-instrumentation
                                                       true}})))
    (is (match? {::stc/instrument? false}
                (run-plugin-hook :kaocha.hooks/config
                                 {:kaocha/cli-options {:stc-instrumentation
                                                       false}}))))
  (testing "::stc/check-asserts?"
    (is (match? {::stc/check-asserts? nil}
                (run-plugin-hook :kaocha.hooks/config {})))
    (is (match? {::stc/check-asserts? true}
                (run-plugin-hook :kaocha.hooks/config
                                 {::stc/check-asserts? true})))
    (is (match? {::stc/check-asserts? true}
                (run-plugin-hook :kaocha.hooks/config
                                 {::stc/check-asserts? true
                                  :kaocha/cli-options  {}})))
    (is (match? {::stc/check-asserts? false}
                (run-plugin-hook :kaocha.hooks/config
                                 {::stc/check-asserts? true
                                  :kaocha/cli-options  {:stc-asserts false}}))))
  (testing "::stc/opts"
    (testing "when there is no existing test suite"
      (is (match? {::stc/opts {}} (run-plugin-hook :kaocha.hooks/config {})))
      (is (match? {::stc/opts {:num-tests 5}}
                  (run-plugin-hook :kaocha.hooks/config
                                   {:kaocha/cli-options {:stc-num-tests 5}})))
      (is (match? {::stc/opts {}} (run-plugin-hook :kaocha.hooks/config {})))
      (is (match? {::stc/opts {:max-size 5}}
                  (run-plugin-hook :kaocha.hooks/config
                                   {:kaocha/cli-options {:stc-max-size 5}}))))
    (testing "when there is no existing test suite"
      (is (match? {::stc/opts {:num-tests 5}}
                  (run-plugin-hook
                   :kaocha.hooks/config
                   {:kaocha/tests       [{::stc/opts {:num-tests 1000}}]
                    :kaocha/cli-options {:stc-num-tests 5}})))
      (is (match? {::stc/opts {:max-size 5}}
                  (run-plugin-hook
                   :kaocha.hooks/config
                   {:kaocha/tests       [{::stc/opts {:max-size 1000}}]
                    :kaocha/cli-options {:stc-max-size 5}}))))))

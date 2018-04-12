(ns lambdaisland.kaocha.integration-test
  (:require [clojure.test :refer :all]
            [clojure.java.shell :as shell]))

(defn invoke-runner [& args]
  (apply shell/sh "clj" "-m" "lambdaisland.kaocha.runner" args))

(deftest command-line-runner-test
  (testing "it lets you specifiy the test suite name"
    (is (= {:exit 0
            :out "\nTesting foo.bar-test\n\nRan 1 tests containing 1 assertions.\n0 failures, 0 errors.\n"
            :err ""}
           (invoke-runner "--config-file" "fixtures/tests.edn" "a")))))

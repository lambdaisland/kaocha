Feature: Selecting test suites

  Scenario: Specifying a test suite on the command line
    Given the following test configuration
      """clojure
      #kaocha/v1
      {:tests [{:id :aaa
                :test-paths ["tests/aaa"]}
               {:id :bbb
                :test-paths ["tests/bbb"]}]}
      """
    And the file "tests/aaa/aaa_test.clj" containing
      """clojure
      (ns aaa-test (:require [clojure.test :refer :all]))
      (deftest foo-test (is true))
      """
    And the file "tests/bbb/bbb_test.clj" containing
      """clojure
      (ns bbb-test (:require [clojure.test :refer :all]))
      (deftest bbb-test (is true))
      """
    When I run Kaocha with "aaa --reporter documentation"
    And the output should contain
      """
      aaa-test
        foo-test
      """
    And the output should not contain
      """
      bbb-test
      """

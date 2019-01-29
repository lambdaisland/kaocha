Feature: CLI: Selecting test suites

  Each test suite has a unique id, given as a keyword in the test configuration.
  You can supply one or more of these ids on the command line to run only those
  test suites.

  Background: Given two test suites, `:aaa` and `:bbb`
    Given a file named "tests.edn" with:
      """clojure
      #kaocha/v1
      {:tests [{:id :aaa
                :test-paths ["tests/aaa"]}
               {:id :bbb
                :test-paths ["tests/bbb"]}]}
      """

    And a file named "tests/aaa/aaa_test.clj" with:
      """clojure
      (ns aaa-test (:require [clojure.test :refer :all]))
      (deftest foo-test (is true))
      """

    And a file named "tests/bbb/bbb_test.clj" with:
      """clojure
      (ns bbb-test (:require [clojure.test :refer :all]))
      (deftest bbb-test (is true))
      """

  Scenario: Specifying a test suite on the command line
    When I run `bin/kaocha aaa --reporter documentation`
    And the output should contain:
      """
      aaa-test
        foo-test
      """
    And the output should not contain
      """
      bbb-test
      """

  Scenario: Specifying a test suite using keyword syntax
    When I run `bin/kaocha :aaa --reporter documentation`
    And the output should contain:
      """
      aaa-test
        foo-test
      """
    And the output should not contain
      """
      bbb-test
      """

  Scenario: Specifying an unkown suite
    When I run `bin/kaocha suite-name`
    Then the output should contain:
      """
      No such suite: :suite-name, valid options: :aaa, :bbb.
      """

Feature: `--reporter` option

  The progress and summary printed by Kaocha are done by one or more "reporter"
  functions. A reporter can be specified with the `--reporter` option followed
  by a fully qualified function name.

  Reporters in the `kaocha.report` namespace can be specified without a
  namespace prefix.

  Background:
    Given the default test configuration
    And the following test namespace
      """clojure
      (ns kaocha.reporter-test
        (:require [clojure.test :refer :all]))

      (deftest test-1
        (is (= 1 0)))

      (deftest test-2
        (is true)
        (is (throw (Exception. "")))
        (is true))

      (deftest test-3
        (is true))
      """

  Scenario: Using a fully qualified function as a reporter
    When I run Kaocha with "--reporter kaocha.report/documentation"
    And the output should contain
      """
      kaocha.reporter-test
        test-1 FAIL
        test-2 ERROR
        test-3
      """

  Scenario: Specifying a reporter via shorthand
    When I run Kaocha with "--reporter documentation"
    Then the exit-code should be 2
    And the output should contain
      """
      kaocha.reporter-test
        test-1 FAIL
        test-2 ERROR
        test-3
      """

  Scenario: Using a reporter which does not exist
    When I run Kaocha with "--reporter does/not-exist"
    Then stderr should contain
      """
      ERROR: Failed to resolve reporter var: does/not-exist
      """

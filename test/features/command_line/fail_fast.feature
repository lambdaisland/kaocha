Feature: CLI: `--fail-fast` option

  Kaocha by default runs all tests it can find, providing a final summary on
  failures and errors when all tests have finished. With the `--fail-fast`
  option the test run will be interrupted as soon as a single failure or error
  has occured. Afterwards a summary of the test run so far is printed.

  Scenario: Failing fast
    Given a file named "test/my/project/fail_fast_test.clj" with:
      """clojure
      (ns my.project.fail-fast-test
        (:require [clojure.test :refer :all]))

      (deftest test-1
        (is true))

      (deftest test-2
        (is true)
        (is false)
        (is true))

      (deftest test-3
        (is true))
      """
    When I run `bin/kaocha --fail-fast`
    Then the exit-code should be 1
    And the output should contain:
      """
      [(..F)]

      FAIL in my.project.fail-fast-test/test-2 (fail_fast_test.clj:9)
      expected: false
        actual: false
      2 tests, 3 assertions, 1 failures.
      """

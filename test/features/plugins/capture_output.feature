Feature: Plugin: Capture output

  Kaocha has a plugin which will capture all output written to stdout or stderr
  during the test run. When tests pass this output is hidden, when they fail the
  output is made visible to help understand the problem.

  This plugin is loaded by default, but can be disabled with `--no-capture-output`

  Scenario: Show output of failing test
    Given a file named "test/sample_test.clj" with:
      """ clojure
      (ns sample-test
        (:require [clojure.test :refer :all]))

      (deftest stdout-pass-test
        (println "You peng zi yuan fang lai")
        (is (= :same :same)))

      (deftest stdout-fail-test
        (println "Bu yi le hu?")
        (is (= :same :not-same)))
      """
    When I run `bin/kaocha`
    Then the output should contain:
      """
      FAIL in sample-test/stdout-fail-test (sample_test.clj:10)
      Expected:
        :same
      Actual:
        -:same +:not-same
      ╭───── Test output ───────────────────────────────────────────────────────
      │ Bu yi le hu?
      ╰─────────────────────────────────────────────────────────────────────────
      2 tests, 2 assertions, 1 failures.
      """

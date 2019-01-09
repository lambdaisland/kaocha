Feature: Marking tests as pending

  Pending tests are tests that are not yet implemented, or that need fixing, and
  that you don't want to forget about. Pending tests are similar to skipped
  tests (see the section on "Filtering"), in that the runner will skip over them
  without trying to run them.

  The difference is that pending tests are explicitly reported in the test
  result. At the end of each test run you get to see the number of pending
  tests, followed by a list of their test ids and file/line information. This
  constant reminder is there to make sure pending tests are not left
  unaddressed.

  Add the `^:kaocha/pending` metadata to a test to mark it as pending. The
  metadata check is done inside the Kaocha runner itself, not in the specific
  test type implementation, so this metadata is supported on any test type that
  allows setting metadata tags, including ClojureScript and Cucumber tests.

  Scenario: Marking a test as pending
    Given a file named "test/sample/sample_test.clj" with:
    """clojure
    (ns sample.sample-test
      (:require [clojure.test :refer :all]))

    (deftest ^:kaocha/pending my-test)
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    [(P)]
    1 tests, 0 assertions, 1 pending, 0 failures.

    PENDING sample.sample-test/my-test (sample/sample_test.clj:4)
    """

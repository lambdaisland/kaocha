Feature: Focusing based on metadata

  You can limit the test run based on test's metadata. How to associate metadata
  with a test depends on the test type, for `clojure.test` type tests metadata
  can be associated with a test var or test namespace.

  Using the `--focus-meta` command line flag, or `:kaocha.filter/focus-meta` key
  in test suite configuration, you can limit the tests being run to only those
  where the given metadata key has a truthy value associated with it.

  Background: Some tests with metadata
    Given a file named "test/my/project/sample_test.clj" with:
      """clojure
      (ns ^:xxx my.project.sample-test
        (:require [clojure.test :refer :all]))

      (deftest some-test
        (is (= 1 1)))

      (deftest other-test
        (is (= 2 2)))
      """
    And a file named "test/my/project/other_sample_test.clj" with:
      """clojure
      (ns my.project.other-sample-test
        (:require [clojure.test :refer :all]))

      (deftest ^:yyy other-test
        (is (= 3 3)))
      """

  Scenario: Focusing by metadata from the command line
    When I run `bin/kaocha --focus-meta :xxx --reporter documentation`
    Then the output should contain:
      """
      --- unit (clojure.test) ---------------------------
      my.project.sample-test
        other-test
        some-test

      2 tests, 2 assertions, 0 failures.
      """

  Scenario: Focusing on a test group by metadata from the command line
    When I run `bin/kaocha --focus-meta :yyy --reporter documentation`
    Then the output should contain:
      """
      --- unit (clojure.test) ---------------------------
      my.project.other-sample-test
        other-test

      1 tests, 1 assertions, 0 failures.
      """

  Scenario: Focusing based on metadata via configuration
    Given a file named "tests.edn" with:
      """ edn
      #kaocha/v1
      {:tests [{:kaocha.filter/focus-meta [:yyy]}]
       :color? false
       :randomize? false}
      """
    When I run `bin/kaocha --reporter documentation`
    Then the output should contain:
      """
      --- unit (clojure.test) ---------------------------
      my.project.other-sample-test
        other-test

      1 tests, 1 assertions, 0 failures.
      """

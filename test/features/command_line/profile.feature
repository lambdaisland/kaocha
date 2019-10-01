Feature: CLI: `--profile` option

  The `--profile KEYWORD` flags sets the profile that is used to read the
  `tests.edn` configuration file. By using the `#profile {}` tagged reader
  literal you can provide different configuration values for different
  scenarios.

  If the `CI` environment value is set to `"true"`, as is the case on most CI
  platforms, then the profile will default to `:ci`. Otherwise it defaults to
  `:default`.

  Scenario: Specifying profile on the command line
    Given a file named "tests.edn" with:
      """ clojure
      #kaocha/v1
      {:reporter #profile {:ci kaocha.report/documentation
                           :default kaocha.report/dots}}
      """
    And a file named "test/my/project/my_test.clj" with:
      """clojure
      (ns my.project.my-test
        (:require [clojure.test :refer :all]))

      (deftest test-1
        (is true))
      """
    When I run `bin/kaocha --profile :ci`
    And the output should contain:
      """
      --- unit (clojure.test) ---------------------------
      my.project.my-test
        test-1
      """

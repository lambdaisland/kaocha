
Feature: Syntax errors are preserved
  Syntax errors should be passed along.
  Scenario: Show output of failing test
    Given a file named "test/sample_test.clj" with:
      """ clojure
      (ns sample-test
        (:require [clojure.test :refer :all]))

      stray-symbol

      (deftest stdout-pass-test
        (is (= :same :same)))
      """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    Exception: clojure.lang.Compiler$CompilerException: Syntax error compiling at (sample_test.clj:0:0).
    """

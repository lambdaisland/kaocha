Feature: `clojure.test` assertion extensions

  \When running `clojure.test` based tests through Kaocha, some of the behavior
  is a little different. Kaocha tries to detect certain scenarios that are
  likely mistakes which make a test pass trivially, and turns them into errors
  so you can investigate and see what's up.

  Kaocha will also render failures differently, and provides extra multimethods
  to influence how certain failures are presented.

  Scenario: Detecting missing assertions
    Given a file named "test/sample_test.clj" with:
      """ clojure
      (ns sample-test
        (:require [clojure.test :refer :all]))

      (deftest my-test
        (= 4 5))
      """
    When I run `bin/kaocha`
    Then the output should contain:
      """ text
      FAIL in sample-test/my-test (sample_test.clj:4)
      Test ran without assertions. Did you forget an (is ...)?
      """

  Scenario: Detecting single argument `=`
    Given a file named "test/sample_test.clj" with:
      """ clojure
      (ns sample-test
        (:require [clojure.test :refer :all]))

      (deftest my-test
        (is (= 4) 5))
      """
    When I run `bin/kaocha`
    Then the output should contain:
      """ text
      FAIL in sample-test/my-test (sample_test.clj:5)
      Equality assertion expects 2 or more values to compare, but only 1 arguments given.
      Expected:
        (= 4 arg2)
      Actual:
        (= 4)
      1 tests, 1 assertions, 1 failures.
      """

  Scenario: Pretty printed diffs
    Given a file named "test/sample_test.clj" with:
      """ clojure
      (ns sample-test
        (:require [clojure.test :refer :all]))

      (defn my-fn []
        {:xxx [1 2 3]
         :blue :red
         "hello" {:world :!}})

      (deftest my-test
        (is (= {:xxx [1 3 4]
                "hello" {:world :?}}
               {:xxx [1 2 3]
                :blue :red
                "hello" {:world :!}})))
      """
    When I run `bin/kaocha`
    Then the output should contain:
      """ text
      FAIL in sample-test/my-test (sample_test.clj:10)
      Expected:
        {"hello" {:world :?}, :xxx [1 3 4]}
      Actual:
        {"hello" {:world -:? +:!}, :xxx [1 +2 3 -4], +:blue :red}
      1 tests, 1 assertions, 1 failures.
      """

Feature: Plugin: Bindings

  The `bindings` plugin allows you to configure dynamic var bindings from
  `tests.edn`, so they are visible during the test run.

  Scenario: Implementing a hook
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:plugins [:kaocha.plugin/bindings]
     :kaocha/bindings {my.foo-test/*a-var* 456}}
    """
    And a file named "test/my/foo_test.clj" with:
    """ clojure
    (ns my.foo-test
      (:require [clojure.test :refer :all]))

    (def ^:dynamic *a-var* 123)

    (deftest var-test
      (is (= 456 *a-var*)))
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    1 tests, 1 assertions, 0 failures.
    """

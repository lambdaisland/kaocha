
Feature: Configuration: Warnings

  Kaocha will warn about common mistakes.


  Scenario: No config
    Given a file named "test/my/foo_test.clj" with:
    """ clojure
    (ns my.foo-test
      (:require [clojure.test :refer :all]))

    (deftest var-test
      (is (= 456 456)))
    """
    When I run `bin/kaocha -c alt-tests.edn`
    Then stderr should contain:
    """
    Did not load a configuration file and using the defaults.
    """
  Scenario: Warn about bad configuration
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:plugins notifier}
    """
    And a file named "test/my/foo_test.clj" with:
    """ clojure
    (ns my.foo-test
      (:require [clojure.test :refer :all]))

    (deftest var-test
      (is (= 456 456)))
    """
    When I run `bin/kaocha`
    Then stderr should contain:
    """
    Invalid configuration file:
    """

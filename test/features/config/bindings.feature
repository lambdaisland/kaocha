Feature: Configuration: Bindings

  You can configure dynamic vars from `tests.edn`, these will be bound to the
  given values during the complete loading and running of the tests.

  Technically they are bound after the config step, so `config` hooks will not
  see the given values, while `pre-load` up to `post-run` will.

  The `:bindings` configuration key takes a map from var name to value.

  Some suggestions of things you can do with this:

  - `kaocha.stacktrace/*stacktrace-filters* []` disable filtering of
    stacktraces, showing all stack frames
  - `kaocha.stacktrace/*stacktrace-stop-list* []` disable the shortening
    of the stacktrace (by default stops printing when it sees "kaocha.ns")
  - `clojure.pprint/*print-right-margin* 120` Make pretty printing use longer
    line lengths
  - `clojure.test.check.clojure-test/*report-completion* false, clojure.test.check.clojure-test/*report-trials* false`
    Make test.check less noisy.

  Scenario: Binding dynamic vars
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:bindings {my.foo-test/*a-var* 456}}
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

  Scenario: Stacktrace filtering
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:bindings {kaocha.stacktrace/*stacktrace-filters* ["clojure.core"]}}
    """
    And a file named "test/my/erroring_test.clj" with:
    """ clojure
    (ns my.erroring-test
      (:require [clojure.test :refer :all]))

    (deftest stacktrace-test
      (is (throw (java.lang.Exception.)))
    
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    at clojure.lang
    """
    And the output should not contain
    """
    at clojure.core
    """

  Scenario: Stacktrace filtering turned off
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:bindings {kaocha.stacktrace/*stacktrace-filters* []}}
    """
    And a file named "test/my/erroring_test.clj" with:
    """ clojure
    (ns my.erroring-test
      (:require [clojure.test :refer :all]))

    (deftest stacktrace-test
      (is (throw (java.lang.Exception.)))
    
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    at clojure.core
    """

  Scenario: Stacktrace shortening
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:bindings {kaocha.stacktrace/*stacktrace-stop-list* ["kaocha.ns"]}}
    """
    And a file named "test/my/erroring_test.clj" with:
    """ clojure
    (ns my.erroring-test
      (:require [clojure.test :refer :all]))

    (deftest stacktrace-test
      (is (throw (java.lang.Exception.)))
    
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    (Rest of stacktrace elided)
    """
    And the output should not contain
    """
    at kaocha.ns
    """

  Scenario: Disable stacktrace shortening
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:bindings {kaocha.stacktrace/*stacktrace-filters* []
                kaocha.stacktrace/*stacktrace-stop-list* []}}
    """
    And a file named "test/my/erroring_test.clj" with:
    """ clojure
    (ns my.erroring-test
      (:require [clojure.test :refer :all]))

    (deftest stacktrace-test
      (is (throw (java.lang.Exception.)))
    
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    at kaocha.runner
    """
    And the output should not contain
    """
    (Rest of stacktrace elided)
    """

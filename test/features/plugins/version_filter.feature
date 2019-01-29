Feature: Plugin: Clojure/Java Version filter

  The `version-filter` plugin will look for test metadata specifying the minimum
  or maximum version of Clojure or Java the test is designed to work with, and
  skip the test unless it falls within the range specified.

  The recognized metadata keys are `:min-clojure-version`,
  `:max-clojure-version`, `:min-java-version`, and `:max-java-version`. The
  associated value is a version string, such as `"1.10.0"`.

  You can set both a minimum and a maximum to limit to a certain range. The
  boundaries are always inclusive, so `^{:max-clojure-version "1.9"}` will run
  on Clojure `1.9.*` or earlier.

  Specificty matters, a test with a max version of `"1.10" will also run on
  version `"1.10.2"`, whereas if the max version is `"1.10.0"` it will not.

  Note that the Java version is based on the "java.runtime.version" system
  property. Before Java 9 this was the so called "developer version", which
  started with `1.`, e.g. `"1.8.0"`, so Java (JDK) versions effectivel jumped
  from `1.8` to `9`.
  [1](https://blogs.oracle.com/java-platform-group/a-new-jdk-9-version-string-scheme)
  [2](https://en.wikipedia.org/wiki/Java_version_history#Versioning_change)

  Scenario: Enabling in `tests.edn`
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:plugins [:kaocha.plugin/version-filter]
     :color? false}
    """
    And a file named "test/my/sample_test.clj" with:
    """ clojure
    (ns my.sample-test
      (:require [clojure.test :refer :all]))

    (deftest ^{:max-java-version "1.7"} this-test-gets-skipped
      (is false))

    (deftest ^{:min-clojure-version "1.6.0"} this-test-runs
      (is true))
    """
    When I run `bin/kaocha --reporter documentation`
    Then the output should contain:
    """
    --- unit (clojure.test) ---------------------------
    my.sample-test
      this-test-runs

    1 tests, 1 assertions, 0 failures.
    """

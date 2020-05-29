Feature: Plugin: Hooks

  The hooks plugin allows hooking into Kaocha's process with arbitrary
  functions. This is very similar to using writing a plugin, but requires less
  boilerplate.

  See the documentation for extending Kaocha for a description of the different
  hooks. The supported hooks are: pre-load, post-load, pre-run, post-run,
  pre-test, post-test, pre-report.

  The hooks plugin also provides two extra hooks at the test suite level,
  `:kaocha.hooks/before` and `:kaocha.hooks/after`, which run respectively
  before any pre-test, and after any post-test hooks.

  Hooks can be specified as a fully qualified symbol, or a collection thereof.
  The referenced namespaces will be loaded during the config phase.

  Scenario: Implementing a hook
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:plugins [:kaocha.plugin/hooks]
     :kaocha.hooks/pre-test [my.kaocha.hooks/sample-hook]}
    """
    And a file named "src/my/kaocha/hooks.clj" with:
    """ clojure
    (ns my.kaocha.hooks)

    (println "ok")

    (defn sample-hook [test test-plan]
      (if (re-find #"fail" (str (:kaocha.testable/id test)))
        (assoc test :kaocha.testable/pending true)
        test))
    """
    And a file named "test/sample_test.clj" with:
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
    PENDING sample-test/stdout-fail-test (sample_test.clj:8)
    """

  Scenario: Implementing a test-suite specific hook
    Given a file named "tests.edn" with:
    """ clojure
#kaocha/v1
{:plugins [:kaocha.plugin/hooks]
 :tests [{:id :unit
          :kaocha.hooks/before [my.kaocha.hooks/sample-before-hook]
          :kaocha.hooks/after  [my.kaocha.hooks/sample-after-hook]}]}
    """
    And a file named "src/my/kaocha/hooks.clj" with:
    """ clojure
    (ns my.kaocha.hooks)

    (defn sample-before-hook [suite test-plan]
      (println "before suite:" (:kaocha.testable/id suite))
      suite)

    (defn sample-after-hook [suite test-plan]
      (println "after suite:" (:kaocha.testable/id suite))
      suite)
    """
    And a file named "test/sample_test.clj" with:
    """ clojure
    (ns sample-test
      (:require [clojure.test :refer :all]))

    (deftest stdout-pass-test
      (println "You peng zi yuan fang lai")
      (is (= :same :same)))
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    before suite: :unit
    [(.)]after suite: :unit
    """

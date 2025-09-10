Feature: Orchestra (spec instrumentation)

  You can enable spec instrumentation of your functions before running
  tests with the `:kaocha.plugin/orchestra` plugin. This uses the
  [Orchestra](https://github.com/jeaye/orchestra) library to instrument
  `:args`, `:ret`, and `:fn` specs.

  You can use the `:kaocha.plugin/preloads` plugin to ensure namespaces
  are required (similar to ClojureScript's preloads feature). This is
  useful to ensure that your specs required before the orchestra plugin
  instruments your functions.

  Scenario: Enabling Orchestra
    Given a file named "tests.edn" with:
    """ clojure
    #kaocha/v1
    {:plugins [:orchestra
               :preloads]
     :kaocha.plugin.preloads/ns-names [my.specs]
     :color? false}
    """
    And a file named "test/orchestra_test.clj" with:
    """ clojure
    (ns orchestra-test
      (:require [clojure.test :refer :all]
                [clojure.spec.alpha :as spec]))

    (defn simple-fn []
      "x")

    (spec/fdef simple-fn :ret :simple/int)

    (deftest spec-fail-test
      (is (= "x" (simple-fn)) "Just testing simple-fn"))
    """
    And a file named "src/my/specs.clj" with:
    """ clojure
    (ns my.specs
      (:require [clojure.spec.alpha :as spec]))

    (spec/def :simple/int int?)
    """
    When I run `bin/kaocha`
    Then the output should contain:
    """
    ERROR in orchestra-test/spec-fail-test (orchestra_test.clj:11)
    Just testing simple-fn
    Call to orchestra-test/simple-fn did not conform to spec.
    orchestra_test.clj:11

    -- Spec failed --------------------

    Return value

      "x"

    should satisfy

      int?

    -- Relevant specs -------

    :simple/int:
      clojure.core/int?

    -------------------------
    Detected 1 error
    """

Plugin: Orchestra (spec instrumentation)

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
    #kaocha/v1 {:plugins [:kaocha.plugin/orchestra
                          :kaocha.plugin/preloads]
                :kaocha.plugin.preloads/ns-names [thanks.spec]}
    """
    And a file named "test/orchestra_test.clj" with:
    """ clojure
    (ns orchestra-test
      (:require [clojure.test :refer :all]
                ))

    (defn simple-fn []
      "x")

    (s/fdef simple-fn :ret :simple/int)

    (deftest spec-fail-test
      (is (= "x" (simple-fn))))
    """
    And a file named "src/spec.clj" with:
    """ clojure
    (ns spec
      (:require [clojure.spec.alpha :as s]))

    (s/def :simple/int int?)
    """
    When I run `bin/kaocha`
    And I run `cat /tmp/kaocha.txt`
    Then the output should contain:
    ``` nil
    ⛔️ Failing
    1 tests, 1 failures.
    true
    1
    critical
    ```





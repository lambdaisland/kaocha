Feature: Automatic spec test check generation

  Kaocha can discover all of your fdefs and generate `clojure.spec.test.check`
  tests for them. This saves you the trouble of writing your own boilerplate,
  and gives you the truly "free" generative testing that clojure.spec promises.

  There are two ways you can use this feature:

  1. Adding `:kaocha.type/spec.test.check` test suites to your `tests.edn`:
    - `:kaocha.testable/type` = :kaocha.type/spec.test.check
    - `:kaocha/source-paths`: Normally your fdefs are with your code, so this
      can probably be left defaulted at `["src"]`
    - `:kaocha.spec.test.check/checks`: Optional. If you want to
      orchestrate multiple "sets" of checks with differing parameters, you can
      specify them here. This is a collection of checks, each check being a map
      which may contain the following optional keys:
      - `:kaocha.spec.test.check/syms`: Currently your only options are either
        `:all-fdefs` (default) or to provide a set of the symbols for the fdefs
        which you want to test. Eventually we will add `:other-fdefs` to select
        all the fdefs that were not specifically mentioned in other checks.
      - `:kaocha.spec.test.check/instrument?` Turn on orchestra instrumentation
        during fdef checks
      - `:kaocha.spec.test.check/check-asserts?` Run s/check-asserts during fdef
        checks
      - `:clojure.spec.test.check/opts`: A map containing any of:
        - `:num-tests`: Test iterations per fdef
        - `:max-size`: Maximum length of generated collections
    - All of the keys within each check can also be given in the top-level test
      suite map to be merged by default into all checks.
  2. The `kaocha.plugin.alpha/spec-test-check` plugin
    - This provides a sane default test suite for automatically checking all of
      your fdefs. Spec test checking can be configured with more granularity in
      tests.edn (as above), but the plugin exists for easy and simplistic CLI
      control.
    - Regardless of whether you add the test suite(s) to `tests.edn` yourself,
      you can also use this plugin to forceably override certain test
      parameters:
        - `--[no-]stc-instrumentation` = `:kaocha.spec.test.check/instrument?`
        - `--[no-]stc-asserts` = `:kaocha.spec.test.check/check-asserts?`
        - `--stc-num-tests NUM` = `:num-tests`
        - `--stc-max-size SIZE` = `:max-size`
    - By default, this plugin also adds `:no-gen` to `:kaocha.filter/skip-meta`.
      You might want to decorate an fdef-ed function with `^:no-gen` if there is
      either no good generator for one or more of its arguments or if the
      function is side-effectful.

  Scenario: Detects and checks fdefs using tests.edn
    Given a file named "tests.edn" with:
      """ clojure
      #kaocha/v1
      {:tests [{:type :kaocha.type/spec.test.check
                :id   :generative-fdef-checks}]}
      """
    Given a file named "src/sample.clj" with:
      """ clojure
      (ns sample
        (:require [orchestra.core :refer [defn-spec]]))

      (defn-spec ok-fn  boolean? [x int?] true)
      (defn-spec bad-fn boolean? [x int?] x)
      """
    When I run `bin/kaocha --reporter kaocha.report/documentation --no-randomize --no-color`
    Then the output should contain:
      """ text
      --- generative-fdef-checks (clojure.spec.test.check) ---------------------------
      sample
        sample/bad-fn FAIL
        sample/ok-fn


      FAIL in sample/bad-fn (sample.clj:5)
      == Checked sample/bad-fn ====================

      -- Function spec failed -----------

        (sample/bad-fn 0)

      returned an invalid value.

        0

      should satisfy

        boolean?

      -------------------------
      Detected 1 error

      expected: boolean?
        actual: 0
      2 tests, 2 assertions, 1 failures.
      """

  Scenario: Plugin: kaocha.plugin.alpha/spec-test-check
    Given a file named "src/sample.clj" with:
      """ clojure
      (ns sample
        (:require [orchestra.core :refer [defn-spec]]))

      (defn-spec ok-fn  keyword? [k keyword?] k)
      (defn-spec bad-fn boolean? [k keyword?] (ok-fn k))
      """
    When I run `bin/kaocha --reporter kaocha.report/documentation --no-randomize --no-color --plugin kaocha.plugin.alpha/spec-test-check`
    Then the output should contain:
      """ text
      sample
        sample/bad-fn FAIL
        sample/ok-fn
      """"

Feature: CLI: Print the Kaocha configuration

  A Kaocha test run starts with building up a Kaocha configuration map, based on
  default values, the contents of `tests.edn`, command line flags, and active
  plugins.

  Debugging issues with Kaocha often starts with inspecting the configuration,
  which is why a `--print-config` flag is provided. This builds up the
  configuration from any available sources, runs it through any active plugins,
  and then pretty prints the result, an EDN map.

  Scenario: Using `--print-config`
    When I run `bin/kaocha --print-config`
    Then the output should contain:
      """ clojure
      {:kaocha.plugin.randomize/randomize? false,
       :kaocha/reporter [kaocha.report/dots],
       :kaocha/color? false,
       :kaocha/fail-fast? false,
      """
    And the output should contain:
      """ clojure
       :kaocha/tests
       [{:kaocha.testable/type :kaocha.type/clojure.test,
         :kaocha.testable/id :unit,
         :kaocha/ns-patterns ["-test$"],
         :kaocha/source-paths ["src"],
         :kaocha/test-paths ["test"],
         :kaocha.filter/skip-meta [:kaocha/skip]}],
      """

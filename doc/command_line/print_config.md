<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# CLI: Print the Kaocha configuration

A Kaocha test run starts with building up a Kaocha configuration map, based on
  default values, the contents of `tests.edn`, command line flags, and active
  plugins.

  Debugging issues with Kaocha often starts with inspecting the configuration,
  which is why a `--print-config` flag is provided. This builds up the
  configuration from any available sources, runs it through any active plugins,
  and then pretty prints the result, an EDN map.

  Note that the ordering, while not expected to change, is not guaranteed. We
  recommend parsing the configuration as EDN and not relying on order. If you
  are manipulating the output as text (say, on the command line) and can't
  avoid relying on the order, run it through a tool like
  [puget](https://github.com/greglook/puget) or
  [zprint](https://github.com/kkinnear/zprint) that sorts the keys
  alphabetically first.

## Using `--print-config`

- <em>When </em> I run `bin/kaocha --print-config`

- <em>Then </em> the EDN output should contain:

``` clojure
{:kaocha.plugin.randomize/randomize? false,
 :kaocha/reporter [kaocha.report/dots],
 :kaocha/color? false,
 :kaocha/fail-fast? false}
```


- <em>And </em> the EDN output should contain:

``` clojure
 {:kaocha/tests
 [{:kaocha.testable/type :kaocha.type/clojure.test,
   :kaocha.testable/id :unit,
   :kaocha/ns-patterns ["-test$"],
   :kaocha/source-paths ["src"],
   :kaocha/test-paths ["test"],
   :kaocha.filter/skip-meta [:kaocha/skip]}]}
```




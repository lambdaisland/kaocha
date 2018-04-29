# Kaocha [![CircleCI](https://circleci.com/gh/lambdaisland/kaocha.svg?style=svg)](https://circleci.com/gh/lambdaisland/kaocha)

Full featured next generation test runner for Clojure.

## Version information

Kaocha is currently under active development, expect things to change. It's already Useful Software though, and you are welcome to try it out.

``` clojure
;; deps.edn
{:deps {lambdaisland/kaocha {:git/url "https://github.com/lambdaisland/kaocha.git"
                             :sha "..."}}}
```

## Configuration

Before running tests with Kaocha, you should create a `tests.edn` in the root of the project, where you configure your test suites. You can use Kaocha without a `tests.edn`, but adding one is generally considered a Good Idea™.

``` clojure
{;; Configure one or more test suites, like "unit", "integration", "acceptance", etc.
 :suites      [{;; Every suite must have an :id
                :id :unit

                ;; Directories containing files under test. This is used to
                ;; watch for changes, and when doing code coverage analysis
                ;; through Cloverage. These directories are *not* automatically
                ;; added to the classpath.
                :source-paths ["src"]

                ;; Directories containing tests. These will automatically be
                ;; added to the classpath when running this suite.
                :test-paths ["test"]

                ;; Regex patterns to determine whether a namespace contains
                ;; tests.
                :ns-patterns [#"-test$"]
                }]

 ;; Colorize output (use ANSI escape sequences).
 :color       true

 ;; Randomize test order.
 :randomize   true

 ;; Watch the file system for changes and re-run.
 :watch       false

 ;; Specifiy the reporter function that generates output. Must be a namespaced
 ;; symbol, or a vector of symbols. The symbols must refer to vars, which Kaocha
 ;; will make sure are loaded. When providing a vector of symbols, or pointing
 ;; at a var containing a vector, then kaocha will call all referenced functions
 ;; for reporting.
 :reporter    kaocha.report/progress

 ;; Run with a specific random seed. Picked at random by default.
 :seed        151346}}
```

All these configuration keys have default values, shown above, so you can omit most of them, including `:suites`.

Suite-level configuration lke `:test-paths` or `:ns-patterns` can also be specified at the top level of `tests.edn`, these will be used unless a suite specifies its own value for that key.

All configuration keys can be overridden with command line flags. Use `--print-config` to see the final result.

In other words, conceptually configuration is merged like so:

``` clojure
(merge kaocha-defaults tests.edn suite-config command-line-flags)
```

## Reporter

A reporter is a function which takes a single map as argument, with the map having a `:type` key. Kaocha uses the same types as `clojure.test`, but adds `:begin-test-suite` and `:end-test-suite`.

Kaocha contains fine-grained reporters, which you can combine, or mix with your own to get the desired output. A reporter can be either a function, or a sequence of reporters, which will all be called in turn. For instance, the default Kaocha reporter is defined as such:

``` clojure
(ns kaocha.report)

(def progress
  "Reporter that prints progress as a sequence of dots and letters."
  [track
   dots
   result])
```

Other reporters currently implemented include

- `kaocha.report/documentation`

## Usage

The main entry point for Kaocha is the `kaocha.runner` namespace, which you can run with `clojure -m kaocha.runner`, followed by Kaocha command line options, and the names of suites to run.

```
USAGE:

clj -m kaocha.runner [OPTIONS]... [TEST-SUITE]...

  -c, --config-file FILE    tests.edn                            Config file to read.
      --print-config                                             Print out the fully merged and normalized config, then exit.
      --fail-fast                                                Stop testing after the first failure.
      --[no-]color                                               Enable/disable ANSI color codes in output. Defaults to true.
      --[no-]watch                                               Watch filesystem for changes and re-run tests.
      --[no-]randomize                                           Run test namespaces and vars in random order.
      --seed SEED                                                Provide a seed to determine the random order of tests.
      --reporter SYMBOL     kaocha.report/progress  Change the test reporter, can be specified multiple times.
      --source-path PATH    src                                  Path containing code under test.
      --test-path PATH      test                                 Path to scan for test namespaces.
      --ns-pattern PATTERN  -test$                               Regexp pattern to identify test namespaces.
  -H, --test-help                                                Display this help message.

Options may be repeated multiple times for a logical OR effect.
```

It is recommended to create a `:test` alias in `deps.edn`, which adds the Kaocha dependency, and invokes the runner namespace.


``` clojure
;; deps.edn
{:aliases
 {:test
  :extra-deps {lambdaisland/kaocha {:mvn/version "VERSION"}}
  :main-opts ["-m" "kaocha.runner"]}}
```

Now you can run your tests with

``` shell
clojure -A:test
```

## Features

- Configure test suites per project, e.g. unit/functional/integration
- Handle classpath and loading of tests
- Fail fast mode: stop at first failure and print report
- Watch mode: watch the file system for changes and re-run tests
- Detect when interrupted with ctrl-C and print report
- Randomize order of tests to detect ordering dependencies
- Cloverage support
- API usage
- Custom, composable reporters

## License

&copy; Arne Brasseur 2018
Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt

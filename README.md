# Kaocha [![CircleCI](https://circleci.com/gh/lambdaisland/kaocha.svg?style=svg)](https://circleci.com/gh/lambdaisland/kaocha)

Full featured next generation test runner for Clojure.

## Rationale

Kaocha is written for end users to provide all the features that people have come to know and love from testing tools in other languages.

Kaocha is written for tooling authors to provide an extensible, data-driven interface to tests.

These two go hand in hand, by providing a solid and extensible foundation it is easy to provide extra features, different output formats, support for other frameworks, and have these things work together instead of living in their own world.

## Current status

Kaocha is a work in progress. Focus so far has been on internal APIs, and on the data formats for configuration, test plan, and test results. These things are not expected to significantly change any more.

There is still more work to be done for framework support, output formats, and provided plugins, and default behavior when no explicit configuration is provided will still change.

Kaocha is not officially released yet, but it is being used in production, and you can use it too if you're willing to take a moment to learn how it works, and to keep up with changes as we iron things out. Keep an eye on the CHANGELOG to learn about breaking changes.

## Getting Started

Currently the standard way of using Kaocha is through a `:test` alias that adds the dependency, and invokes the Kaocha runner.

``` clojure
;; deps.edn
{:aliases
 {:test
  {:extra-deps {lambdaisland/kaocha
                {:git/url "https://github.com/lambdaisland/kaocha.git"
                 :sha "..."}}
   :main-opts ["-m" "kaocha.runner"]}}}
```

This lets you invoke Kaocha with `clj -A:test`.

## Configuration

Before running tests with Kaocha, you should create a `tests.edn` in the root of the project, where you configure your test suites. `tests.edn` defines your *test configuration*, the first type of data structure used by Kaocha. You can define your configuration in full, but it's recommended to start with the `#kaocha {}` reader literal to provide defaults.

```
;; tests.edn
#kaocha {}
```

Chances are this is all the configuration you need. This sets up a number of defaults, and configures a single test suite, with tests in `test`, and source files in `src`. To get a sense of what the actual configuration looks like, you can run kaocha with `--print-config`.

```
clj -A:test --print-config
```

Here's a more full-fledged example, still using `#kaocha {}`.

``` clojure
#kaocha
{:tests [{;; Every suite must have an :id
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
          :ns-patterns [#"-test$"]}]

 :plugins [:kaocha.plugin/print-invocations]

 ;; Colorize output (use ANSI escape sequences).
 :color?      true

 ;; Watch the file system for changes and re-run.
 :watch?      false

 ;; Specifiy the reporter function that generates output. Must be a namespaced
 ;; symbol, or a vector of symbols. The symbols must refer to vars, which Kaocha
 ;; will make sure are loaded. When providing a vector of symbols, or pointing
 ;; at a var containing a vector, then kaocha will call all referenced functions
 ;; for reporting.
 :reporter    kaocha.report/documentation}}
```

All these configuration keys have default values, shown above, so you can omit most of them, including `:tests`.

All configuration keys can be overridden with command line flags. Use `--test-help` to see all options. Use `--print-config` to see the final result.

Configuration is read with [Aero](https://github.com/juxt/aero), meaning you have access to reader literals like `#env`, `#merge`, `#ref`, and `#include`.

## Focusing and Skipping

You can "focus" on specific tests, only running the ones that match your criteria, or skip specific tests. This is done either based on the test ID (the namespace of var name), or on metadata (on the namespace or var).

```
# Run a specific var
clj -A:test --focus com.my-app.foo-test/bar-test

# Run all vars in a namespace
clj -A:test --focus com.my-app.foo-test

# Run all vars or namespaces with the :slow-test metadata
clj -A:test --focus-meta :slow-test

# Skip vars or namespaces with this metadata
clj -A:test --skip-meta :slow-test
```

In the test configuration you can also provide these settings, either at the top level, or for a specific suite.

```clojure
#kaocha
{:kaocha.filter/skip-meta [:test/pending]
 :tests [{:id :unit
          :kaocha.filter/skip-meta [:test/slow]}
         {:id :slow-tests
          :kaocha.filter/focus [:test/slow]}]}
```

## Usage

`clj -A:test` can be followed by arguments and flags, e.g.

```
clj -A:test unit --fail-fast --watch
```

`unit` in this case is the id of a test suite, as configured under `:tests`. (If you don't have a `:tests` key in your configuration, it defaults to a single suite called `unit`). In this case Kaocha will only run the tests from the `unit` suite. If you don't provide a test id it will run all tests.

`--fail-fast` and `--watch` are option flags, their usage is explained below.

### Watching for changes

With `--watch` or the `:watch?` configuration key Kaocha will keep running, and
re-run your tests each time a source or test file changes.

### Fail fast

You can make Kaocha stop at the first error or failure with `--fail-fast` /
`:fail-fast?`. This works well in combination with `--watch`.

### Interrupting

When you hit Ctrl-C in the middle of a long test run, Kaocha will gracefully
exit, after first printing an overview of all failures so far, and a test
summary.

## Test plan and test results

Kaocha works in two phases, a load step and a run step. The load step takes the configuration and returns a test plan, the run step takes the test plan and returns a test result. Through various hooks plugins can operate on these data structures to change Kaocha's behavior.

You can see the test plan and test result with `--print-test-plan` and
`--print-result`. These are invaluable tools for better understanding Kaocha's
behavior.

## Extending Kaocha

Kaocha can be extended in three ways

* New test types (implement load + run)
* Plugins
* `clojure.test` style extensions (custom report message types, custom reporters, extending `is`)

### Test types

Kaocha currently provides three test types

* `:kaocha.type/suite`
* `:kaocha.type/ns`
* `:kaocha.type/var`

These are nested, you use suite at the top level, the load step will find
namespaces and vars.

### Plugins

Plugins can hook into the test process at various points

- cli-options
- config
- pre-load
- post-load
- pre-run
- pre-test
- post-test
- post-run
- post-summary

Kaocha currently ships with these plugins

- `:kaocha.plugin/profiling` Show statistics about the slowest tests.
- `:kaocha.plugin/print-invocations` (experimental) Print out command line invocations that run specific failing tests
- `:kaocha.plugin/randomize` (on by default) Randomize the test order
- `:kaocha.plugin/filter` (on by default) Provide support for filtering and focusing

You can enable a plugin in your `tests.edn`

``` clojure
#kaocha
{:plugins [:kaocha.plugin/profiling]}
```

Or from the command line

```
clj -A:test --plugin :kaocha.plugin/profiling
```

### Reporter

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

- `kaocha.report/dots`
- `kaocha.report/documentation`

## Usage

The main entry point for Kaocha is the `kaocha.runner` namespace, which you can run with `clojure -m kaocha.runner`, followed by Kaocha command line options, and the names of suites to run.

```
USAGE:

clj -m kaocha.runner [OPTIONS]... [TEST-SUITE]...

  -c, --config-file FILE  tests.edn               Config file to read.
      --print-config                              Print out the fully merged and normalized config, then exit.
      --print-test-plan                           Load tests, build up a test plan, then print out the test plan and exit.
      --print-result                              Print the test result map as returned by the Kaocha API.
      --fail-fast                                 Stop testing after the first failure.
      --[no-]color                                Enable/disable ANSI color codes in output. Defaults to true.
      --[no-]watch                                Watch filesystem for changes and re-run tests.
      --reporter SYMBOL   kaocha.report/progress  Change the test reporter, can be specified multiple times.
      --plugin KEYWORD                            Load the given plugin.
  -H, --test-help                                 Display this help message.
      --[no-]randomize                            Run test namespaces and vars in random order.
      --seed SEED                                 Provide a seed to determine the random order of tests.
      --skip SYM                                  Skip tests with this ID and their children.
      --focus SYM                                 Only run this test, skip others.
      --skip-meta SYM                             Skip tests where this metadata key is truthy.
      --focus-meta SYM                            Only run tests where this metadata key is truthy.
```

## Features

- Configure test suites per project, e.g. unit/functional/integration
- Handle classpath and loading of tests
- Fail fast mode: stop at first failure and print report
- Watch mode: watch the file system for changes and re-run tests
- Detect when interrupted with ctrl-C and print report
- Randomize order of tests to detect ordering dependencies
- API usage
- Custom, composable reporters

## Tests as data

Kaocha's architecture is based on three types of data: the configuration, then
test plan, and the test result.

The test configuration is what you specify in `tests.edn`, after expansion and
normalization. A `load` operation takes this configuration, finds and loads all
tests therein and returns a test plan. This test plan looks similar to the
configuration, but with more detail. It contains an overview of exactly which
namespaces and vars will be tested.

When this test plan is run you get a test result, this looks similar to the test
plan, but contains detailed information about which tests passed, failed,
whether they threw exceptions, and what they wrote on standard err and out.

How tests are loaded and run is governed by two multimethods, `load-testable`
and `run-testable`, based on the type of the testable. The default type is
`:kaocha.type/suite`, a test suite with a test-path, namespaces and clojure.test
style test vars.

## License

&copy; Arne Brasseur 2018
Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt

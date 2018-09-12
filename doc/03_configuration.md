# 3. Configuration

## Introduction

Kaocha is configured through a `tests.edn` file, typically placed in the root of
the project. Many options can also be specified on the command line, in which
case they take precedence.

Reading `tests.edn` and combining it with command line flags yields the *Test
Configuration*, which is the first main type of data structure that Kaocha uses.
It contains a description of the test suites to run, the plugins to execute, as
well as various options and flags.

Here's an example test configuration with a single test suite:

``` clojure
{:kaocha/tests                       [{:kaocha.testable/type :kaocha.type/clojure.test
                                       :kaocha.testable/id   :unit
                                       :kaocha/ns-patterns   ["-test$"]
                                       :kaocha/source-paths  ["src"]
                                       :kaocha/test-paths    ["test/unit"]}]
 :kaocha/fail-fast?                  false
 :kaocha/color?                      true
 :kaocha/reporter                    [kaocha.report/dots]
 :kaocha/plugins                     [:kaocha.plugin/randomize
                                      :kaocha.plugin/filter
                                      :kaocha.plugin/capture-output
                                      :kaocha.plugin/profiling]
 :kaocha.plugin.randomize/seed       950716166
 :kaocha.plugin.randomize/randomize? true
 :kaocha.plugin.profiling/count      3
 :kaocha.plugin.profiling/profiling? true}
```

Writing a full test configuration by hand is tedious, which is why in
`tests.edn` you can use the `#kaocha {}` tagged reader literal. It allows using
plain instead of namespaced keywords, and provides many default values. If you
have a single test suite with `clojure.test` style tests in the `test`
directory, then you can start out with a `tests.edn` with nothing but

``` clojure
#kaocha {}
```

Try it out! Use `bin/kaocha --print-config` to see the resulting test
configuration.

In general using `#kaocha {}` is highly recommended, however if you need fine
grained control you can write the output of `--print-config` to `tests.edn` and
go from there. The rest of the documentation will generally use the short forms
used in `#kaocha {}`, rather than using fully qualified keywords.

## Test suites

Test suites are a first class concept in Kaocha. This encourages testing at
different levels of abstraction, testing different parts or aspects of the
project, or writing different types of tests.

A common division is splitting tests into "unit" and "integration" or
"acceptance" tests, but these terms don't have strict definitions. It all
depends on what makes sense for your project.

It can also make sense to have test suites for specific parts of an app, or
suites that test a certain interface. You could have a "frontend", "backend",
and "HTTP API" suite.

In Kaocha a test suite has a `:type` and an `:id`. Depending on the type it will
also have other attributes like the directories to look for tests, or how to
discern test from regular namespaces.

Here's an example of a `tests.edn` defining two test suites: one names `:unit`,
which has its test files under `"test/unit"`, and one named `:features`, with
its tests under `"test/features"`.

``` clojure
#kaocha
{:tests [{:id         :unit
          :test-paths ["test/unit"]}
         {:id         :features
          :test-paths ["test/features"]}]}
```

You can now run only the unit tests with `bin/kaocha unit`, or all tests with
`bin/kaocha`.

Because it's using the `#kaocha {}` shorthand these test suites inherit the
defaults, this means they are of type `:kaocha.type/clojure.test`, that they
consider files under `"src"` to be the code under test, and that only namespaces
ending in `-test` are considered test namespaces.

This is what the `:unit` suite looks like after expansion:

```clojure
{:kaocha.testable/type :kaocha.type/clojure.test,
 :kaocha.testable/id   :unit,
 :kaocha/ns-patterns   ["-test$"],
 :kaocha/source-paths  ["src"],
 :kaocha/test-paths    ["test/unit"]}
```

### :kaocha.type/clojure.test

The main test suite type implemented at the moment is one for `clojure.test`,
the testing library included with Clojure itself.

It takes the following configuration options.

- `:ns-patterns`: vector of regular expressions, if one of them matches the
  namespace name then this namespace is considered a test namespace, and will
  get loaded as part of Kaocha's "load" step. Patterns can be given as actual
  regex types or as strings. Defaults to `["-test$"]`
- `:source-paths`: vector of paths containing source code under test. This
  is used to determine which files to watch for changes, and can be used by
  plugins e.g. when doing code coverage analyis. Defaults to `["src"]`
- `:test-paths`: vector of paths containing tests. These paths are added to the
  JVM classpath prior to executing this suite, and they are searched for
  namespaces to load.

## Plugins

Kaocha can be customized and extended with plugins. Some of these are included
with Kaocha, others can be provided by third parties. They are regular Clojure
libraries that follow specific conventions.

A plugin has a name, a fully qualified keyword, and can be added easily.

``` clojure
#kaocha
{:plugins [:kaocha.plugin/profiling]}
```

Some plugins are needed for the normal functioning of Kaocha. These are added
automatically when using the `#kaocha {}` reader literal. They are

- `:kaocha.plugin/randomize`: randomize test order
- `:kaocha.plugin/filter`: allow filtering and "focusing" of tests
- `:kaocha.plugin/capture-output`: implements output capturing during tests

Typically plugins can be configured through `tests.edn` as well as through command line options. Try `bin/kaocha --test-help` to see which command line flags they add, and `--print-config` to see the defa

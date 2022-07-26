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
`tests.edn` you can use the `#kaocha/v1 {}` tagged reader literal. It allows using
plain instead of namespaced keywords, and provides many default values, including:
- source files are in the `src/` folder,
- test files are in the `test/` folder,
- all test namespaces _names_ end with `-test`
(e.g. `my-project.core-test`).
Also, the default test suite id is `:unit` (just `unit` on the command line).

If you
have a single test suite with `clojure.test` style tests in the `test`
directory, then you can start out with a `tests.edn` with nothing but

``` clojure
#kaocha/v1 {}
```

Try it out! Use `bin/kaocha --print-config` to see the resulting test
configuration.

If your tests don't seem to run (outcome is `0 tests, 0 assertions, 0
failures`), you may need to configure one or more [test
suites](https://cljdoc.org/d/lambdaisland/kaocha/1.0.700/doc/3-configuration#test-suites) with the correct paths.


In general using `#kaocha/v1 {}` is highly recommended, however if you need fine
grained control you can write the output of `--print-config` to `tests.edn` and
go from there. The rest of the documentation will generally use the short forms
used in `#kaocha/v1 {}`, rather than using fully qualified keywords.

Configuration is read with [Aero](https://github.com/juxt/aero), meaning you
have access to reader literals like `#env`, `#merge`, `#ref`, and `#include`.

Additionally kaocha supports an extra `#meta-merge` reader tag, that
works similarly to aero's `#merge` but will merge a whole subtree with
the semantics of
[meta-merge](https://github.com/weavejester/meta-merge).  This is
particularly useful for configuring a cascade of includes that
selectively augment and override a base configuration.  e.g. imagine a
base tests.edn file with a `#meta-merge` and an `#include`:

```
#kaocha/v1
#meta-merge [{:tests [{:id         :unit
              :test-paths ["test/unit"]}
             {:id         :features
             :test-paths ["test/features"]}]
             :kaocha/plugins [:kaocha.plugin.some.required/plugin
                              ,,,]
             }

        #include "tests.user.edn"]
```

This setup would allow developers to maintain their own private
`tests.user.edn` file and augment the above configuration with for example
their own set of plugins such as the notifier etc.

The primary advantage to using the `#meta-merge` tag over aero's
`#merge` is that meta-merge will do a deep merge of the tree; meaning
developers can more easily inherit changes to the base configuration.


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

Here's an example of a `tests.edn` defining two test suites: one named `:unit`,
which has its test files under `"test/unit"`, and one named `:features`, with
its tests under `"test/features"`.

``` clojure
#kaocha/v1
{:tests [{:id         :unit
          :test-paths ["test/unit"]}
         {:id         :features
          :test-paths ["test/features"]}]}
```

You can now run only the unit tests with `bin/kaocha unit`, or all tests with
`bin/kaocha`.

Because it's using the `#kaocha/v1 {}` shorthand these test suites inherit the
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

If you don't define any test suites then Kaocha assumes a single `:unit` test suite.

#### Disabling Automatic Classpath Handling

Kaocha will add any entries in `:kaocha/test-paths` to the classpath, if they
aren't on there already. This is done so this information doesn't need to be
duplicated between `tests.edn` and your project configuration (`deps.edn`,
`project.clj`, etc).

However, often people will have these paths in their `deps.edn` anyway (e.g. in
a `:test` or `:dev` profile), so they are accessible for interactive evaluation
(REPL, in-buffer eval). In some cases you also do not *want* Kaocha to add
these, because it can confuse third-party tools. This is because Kaocha needs a
`DynamicClassLoader` to be able to add classpath entries at runtime. If the
current classloader is not a `DynamicClassLoader` then we set one up ourselves,
but there have been cases where this caused issues with other tools.

In these cases you can add `:kaocha.testable/skip-add-classpath? false` to the
test suite definition to disable this behavior.

### :kaocha.type/clojure.test

The main test suite type implemented at the moment is one for `clojure.test`,
the testing library included with Clojure itself.

It takes the following configuration options.

- `:ns-patterns`: vector of patterns. Patterns are _not_ regexes, they are
  _strings_ that get intepreted as regular expressions (do not prepend with
  `#`). If one of them matches the namespace name then this namespace is
  considered a test namespace, and will get loaded as part of Kaocha's "load"
  step.
  Defaults to `["-test$"]`
- `:source-paths`: vector of paths containing source code under test. This
  is used to determine which files to watch for changes, and can be used by
  plugins e.g. when doing code coverage analyis.
  Defaults to `["src"]`
- `:test-paths`: vector of paths containing tests. These paths are added to the
  JVM classpath prior to executing this suite, and they are searched for
  namespaces to load.
  Defaults to `["test"]`

## Plugins

Kaocha can be customized and extended with plugins. Some of these are included
with Kaocha, others can be provided by third parties. They are regular Clojure
libraries that follow specific conventions.

A plugin has a name, a fully qualified keyword, and can be configured easily,
either in `tests.edn`:

``` clojure
#kaocha/v1
{:plugins [:kaocha.plugin/profiling]}
```

or from the command line

``` shell
bin/kaocha --plugin kaocha.plugin/profiling
```

For plugins in the `kaocha.plugin` namespace, the namespace can be ommitted from
the command line:


``` shell
bin/kaocha --plugin profiling
```

Some plugins are needed for the normal functioning of Kaocha. These are added
automatically when using the `#kaocha/v1 {}` reader literal. They are

- `:kaocha.plugin/randomize`: randomize test order ([documentation](08_plugins.md#randomize))
- `:kaocha.plugin/filter`: allow filtering and "focusing" of tests
- `:kaocha.plugin/capture-output`: implements output capturing during tests

Individual plugins can introduce their own configuration options, which can be
specified either in `tests.edn` or on the command line. After enabling a plugin
try `bin/kaocha --test-help` to see which command line flags were added, and use
`--print-config` to see available configuration keys with their default values.

## Reporters

The output that Kaocha generates is dictated by "reporters". Reporters are a
concept from `clojure.test`, but are used in Kaocha regardless of the test type
being run.

A reporter is configured with `--reporter` from the command line, or as
`:reporter` in `tests.edn`. These reporters are Currently provided:

### `kaocha.report/dots`
CLI: `--reporter kaocha.report/dots`
Config: `{:kaocha/reporter [kaocha.report/dots]}`

Print the output as a sequence of dots and other symbols.

- `[` / `]` Start/end suite
- `(` / `)` Start/end namespace
- `.` Pass
- `F` Fail
- `E` Error

Failures with complete output and error information, as well as a general
summary are printed at the end (or when `Ctrl-C` is pressed). This is a great
reporter for when you want it concise but still information-rich.

This is a great all-around reporter. It's concise, but still rich in information.

```
[(.)(..F)(....)(..E..E)(...)(....)(.)(..)(............)(...)(...........)][(.....)]
19 test vars, 55 assertions, 2 errors, 1 failures.
```

### `kaocha.report/documentation`

CLI: `--reporter kaocha.report/documentation`
Config: `{:kaocha/reporter [kaocha.report/documentation]}`

Provides detailed output of test namespaces, vars, and testing blocks. If you
make good use of `clojure.test`'s facilities this can be very informative. A good choice for use on CI.

```
--- :unit ---------------------------
kaocha.type.var-test
  run-test
    a passing test var
    a failing test var
    an erroring test var
    multiple assertions
    early exit FAIL
      early exit - exception ERROR
```

### `kaocha.report.progress/report`

CLI: `--reporter kaocha.report.progress/report`
Config: `{:kaocha/reporter [kaocha.report.progress/report]}`

Prints a separate progress bar for each test suite, with progress percentage,
and the completed/total number of test vars.

Turns red when a test has failed.

``` clojure
integration:   100% [==================================================] 1/1
       unit:   100% [==================================================] 18/18

19 test vars, 55 assertions, 0 failures.
```

### `kaocha.report/tap`

Reporter that outputs TAP (Test Anything Protocol). Useful for integrating with
other tools. See also
[kaocha-junit-xml](https://github.com/lambdaisland/kaocha-junit-xml).

### `kaocha.report/debug`

Prints the `clojure.test` style events map directly, with some keys like
`:kaocha/testable` filtered out to prevent it from getting too noisy.

## Profiles

Sometimes a single fixed configuration is not enough. If you run tests in
different contexts, for instance locally vs on a build server, or you need to
support more than one workflow, then it can be useful to be able to vary the
configuration.

For these cases [Aero](https://github.com/juxt/aero) has the concept of
profiles. Here's a typical example. When used locally you want to use a colorful
progress bar, but on CI you want plain text output.

``` clojure
#kaocha/v1
{:reporter #profile {:default kaocha.report/documentation
                     :ci kaocha.report.progress/progress}
 :color? #profile {:default true :ci false}}
```

This will work out of the box, since Kaocha will pick up on the `CI` environment
variable that is set by all major CI providers, but you can also specify the
profile explicitly.

```
bin/kaocha --profile :ci
```

Note that profile combines quite nicely with other Aero reader tags like
`#merge` and `#include`.

```
#kaocha/v1
#merge
[{}
 #profile {:ci {:reporter kaocha.report/documentation
                :color? false}}]
```

```
#kaocha/v1
#profile
{:ci #include "tests.ci.edn"
 :default #include "tests.defaults.edn"}
```

## Example

You should be able to start with a simple `#kaocha/v1 {}`, and leave most
configuration at its default. This is merely an example of what's possible

``` clojure
#kaocha/v1
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

          ;; Regex strings to determine whether a namespace contains
          ;; tests. (use strings, not actual regexes, due to a limitation of Aero)
          :ns-patterns ["-test$"]}]

 :plugins [:kaocha.plugin/print-invocations
           :kaocha.plugin/profiling]

 ;; Colorize output (use ANSI escape sequences).
 :color?      true

 ;; Watch the file system for changes and re-run. You can change this here to be
 ;; on by default, then disable it when necessary with `--no-watch`.
 :watch?      false

 ;; Specifiy the reporter function that generates output. Must be a namespaced
 ;; symbol, or a vector of symbols. The symbols must refer to vars, which Kaocha
 ;; will make sure are loaded. When providing a vector of symbols, or pointing
 ;; at a var containing a vector, then kaocha will call all referenced functions
 ;; for reporting.
 :reporter    kaocha.report/documentation

 ;; Enable/disable output capturing.
 :capture-output? true

 ;; Plugin specific configuration. Show the 10 slowest tests of each type, rather
 ;; than only 3.
 :kaocha.plugin.profiling/count 10}
```

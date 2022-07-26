# 8. Plugins

This section describes plug-ins that are built-in but not enabled by default. Functionality of default plugins is described in the [Running Kaocha CLI](04_running_kaocha_cli.md) section.

For information on *writing* plugins, see the section on [extending Kaocha](09_extending.md).

## Profiling

The profiling plugin outputs a list of the slowest tests for each test type at the end of the test run.

### Enabling

``` shell
bin/kaocha --plugin kaocha.plugin/profiling
```

or

``` clojure #kaocha/v1
{:plugins [:kaocha.plugin/profiling]}
```

### Example output

```
Top 2 slowest kaocha.type/clojure.test (37.97308 seconds, 100.0% of total time)
  integration
    37.06616 seconds average (37.06616 seconds / 1 tests)
  unit
    0.08245 seconds average (0.90692 seconds / 11 tests)

Top 3 slowest kaocha.type/ns (37.71151 seconds, 99.3% of total time)
  kaocha.integration-test
    37.06421 seconds average (37.06421 seconds / 1 tests)
  kaocha.type.var-test
    0.57622 seconds average (0.57622 seconds / 1 tests)
  kaocha.runner-test
    0.03554 seconds average (0.07108 seconds / 2 tests)

Top 3 slowest kaocha.type/var (37.70178 seconds, 99.3% of total time)
  kaocha.integration-test/command-line-runner-test
    37.06206 seconds kaocha/integration_test.clj:25
  kaocha.type.var-test/run-test
    0.57399 seconds kaocha/type/var_test.clj:12
  kaocha.runner-test/main-test
    0.06573 seconds kaocha/runner_test.clj:10
```

### Plugin-specific command line flags

```
--[no-]profiling                  Show slowest tests of each type with timing information.
--profiling-count NUM             Show this many slow tests of each kind in profile results.
```

### Plugin-specific configuration options

Shown with their default values:

```
#kaocha/v1
{:kaocha.plugin.profiling/count 3
 :kaocha.plugin.profiling/profiling? true}
```

## GC Profiling

The gc profiling plugin works like the profiling plugin, but for memory usage.
It works by measuring the amount of memory in use before and after each test.


JVM garbage collectors makes measuring memory usage difficult.  Collection can
happen at any time, including in the middle of a test. That means if a test that
created a lot of garbage, those objects may al be freed in a subsequent test. In
that case, the memory usage will sharply decline during the later test, which
can easily swamp results. In some cases, a garbage collection will free more
memory than was allocated during a test, and the result will be negative.
Additionally, heap measurements are not precise.

If you want to just measure allocations, you can enable the Epsilon garbage
collector, which is a stub garbage collector that doesn't actually look for
garbage or free any memory. While this gives you a reasonably accurate sense of
how much was allocated, raw allocations may not be as important as the rate at
which garbage is being generated and whether the garbage collector can keep up.

Be

### Enabling 


``` clojure #kaocha/v1
{:plugins [:kaocha.plugin/gc-profiling]}
```

### Plugin-specific command line flags   ###

```
      --[no-]gc-profiling             Show the approximate memory used by each test.
      --[no-]gc-profiling-individual      Show the details of individual tests."
```

## Print invocations 

At the end of the test run, print command invocations that can be copy-pasted to re-run only specific failed tests.

### Enabling

``` shell
bin/kaocha --plugin kaocha.plugin/print-invocations
```

or

``` clojure
#kaocha/v1
{:plugins [:kaocha.plugin/print-invocations]}
```

### Example output

``` shell
[(.)(F..)(..)(...)(............)(....)(...........)(......)(....)(.)(...)]

FAIL in (clojure-test-summary-test) (kaocha/history_test.clj:5)
...
18 test vars, 50 assertions, 1 failures.

bin/kaocha --focus 'kaocha.history-test/clojure-test-summary-test'
```

## Notifier

Pop up a system notifications whenever a test run fails or passes.

See [Plugins: Notifier](plugins/notifier_plugin.md)

## Version filter

Skip tests that aren't compatible with the current version of Java or Clojure.

See [Plugins: Version Filter](plugins/version_filter.md)

## Hooks

Write functions that hook into various parts of Kaocha

See [Plugins: Hooks](plugins/hooks_plugin.md)

## Orchestra

Instrument functions with [Orchestra](https://github.com/jeaye/orchestra).

See [Plugins: Hooks](plugins/orchestra_plugin.md)

## Preloads

Preload namespaces. Useful for loading specs and installing instrumentation.

``` clojure
#kaocha/v1
{:plugins [:preloads]
 :kaocha.plugin.preloads/ns-names [my.acme.specs]}
```

## Debug

Inspect Kaocha's process by printing out a message at every single hook. This is
mostly intended for people working on Kaocha or Kaocha plugins, but can be
useful in general as a debugging aid when Kaocha doesn't behave the way you
would expect.

Every time a hook fires, it prints the name of the hook, and a subset of keys of
the first argument passed to the hook (usually the testable). It only prints
some keys so the output isn't too noisy. By defaults only prints
`:kaocha.testable/id` and `:kaocha.testable/type`, or for `pre-report` it prints
`:type`, `:file`, `:line`.

To customize which keys to print, use Kaocha's "bindings" functionality, in `tests.edn`:

``` clojure
:kaocha/bindings {kaocha.plugin.debug/*keys* [,,,]}
```

## Randomize

The randomize plugin picks a random seed during the `config` hook and uses that
seed to randomize the order of test suites, namespaces, and test vars during the
`post-load` hook. 

Randomization can be toggled at the following places.

1. top level config
2. test suite config
3. namespace metadata

At each level, changing the randomize value will override the previously set 
value. For example, you could: 

- randomize by default (top level `:kaocha.plugin.randomize/randomize? true`, 
  which is the default)
- exclude a specific test suite by setting `:kaocha.plugin.randomize/randomize? false`
  on the suite in tests.edn
- but randomize a single namespace within that suite `(ns ^{:kaocha.plugin.randomize/randomize? true} ...)`

Or the inverse (case 2)

- turn it off at the top level
- but turn it on for certain tests suites
- with the exception of certain namespaces

Passing the `--no-randomize` CLI flag will force all randomization to be disabled. 

### Enabling

The randomize plugin is enabled by default. Enable or disable randomization by
setting the `:kaocha.plugin.randomize/randomize?` key in tests.edn at the top-level,
in a specific test suite, or on the metadata of a namespace.

### Plugin-specific command line flags

```shell
--[no-]randomize                  Run test namespaces and vars in random order.
--seed SEED                       Provide a seed to determine the random order of tests.
```

### Plugin-specific configuration options

Shown with their default values:

```clojure
#kaocha/v1
{:kaocha.plugin.randomize/randomize? true}
```

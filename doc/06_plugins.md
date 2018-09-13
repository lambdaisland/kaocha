# 6. Plugins

This section describes plug-ins that are built-in but not enabled by default. Functionality of default plugins is described in the [Running Kaocha CLI](04_running_kaocha_cli.md) section.

For information on *writing* plugins, see the section on [extending Kaocha](7_extending.md).

## Profiling

The profiling plugin will output a list of the slowest tests for each test type at the end of the test run.

Enabling the plugin:

``` shell
bin/kaocha --plugin kaocha.plugin/profiling
```

or

``` clojure
#kaocha
{:plugins [kaocha.plugin/profiling]}
```

Result

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

### Extra command line flags

```
--[no-]profiling                  Show slowest tests of each type with timing information.
--profiling-count NUM             Show this many slow tests of each kind in profile results.
```

### Extra configuration options

Shown with their default values:

```
#kaocha
{:kaocha.plugin.profiling/count 3
 :kaocha.plugin.profiling/profiling? true}
```

## Print invocations


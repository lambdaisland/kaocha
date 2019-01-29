# 4. Running Kaocha CLI

If you followed the [installation](02_installing.md) instructions, you
should have a `bin/kaocha` wrapper ("binstub") in your project that accepts
additional arguments.

The command line runner takes the names of test suites to run, as well as a
number of flags and options. If you don't give it any suite names it runs all
suites.

You can get an overview of all available flags with `--test-help`.

``` shell
bin/kaocha --test-help
```

## tests.edn

Kaocha relies on a `tests.edn` configuration file, see the section on
[configuration](03_configuration.md). To load an alternative configuration, use
the `--config-file` option.

``` shell
bin/kaocha --config-file tests_ci.edn
```

Note that plugins specified in the config file will influence the available
command line options.

## Plugins and reporters

The `--plugin` option lets you activate additional plugins, it is followed by
the plugin's name, a namespaced symbol. This option can be specified multiple
times.

With `--reporter` you can specify an alternative reporter, to change Kaocha's
output.

For example, to see a colorful progress bar, use

``` shell
bin/kaocha --reporter kaocha.report.progress/progress
```

Plugins in the `kaocha.plugin` namespace, and reporters in the `kaocha.report`
namespace can be specified without the namespace.

``` shell
bin/kaocha --plugin profiling --reporter documentation
```

## Fail fast mode

``` shell
bin/kaocha --fail-fast
```

Stop the test run as soon as a single assertion fails or an exception is thrown,
and then print the results so far.

## Randomization

Kaocha by default randomizes the order that tests are run: it picks a random
seed, and uses that to re-order the test suites, namespaces, and test vars.

Tests should be independent, but this is not always the case. This random order
helps to track down unintended dependencies between tests.

The random seed will be printed at the start of the test run. On the same code
base with the same seed you will always get the same test order. This way you
can e.g. reproduce a test run that failed on a build server.

``` shell
bin/kaocha --seed 10761431
```

Use `--no-randomize` to load suites in the order they are specified, and vars in
the order they occur in the source. You can disable randomization in `tests.edn`

## Control output

Kaocha makes liberal use of ANSI escape codes for colorizing the output. If you
prefer or need plain text output use `--no-color`.

By default Kaocha will capture any output that occurs on stdout or stderr during
a test run. Only when a test fails is the captured output printed as part of the
test result summary. This is generally what you want, since this way tests that
pass don't generate distracting noise. If you do want all the output as it
occurs, use `--no-capture`.

## Debug information

`--version` prints version information, whereas `--test-help` will print the
available command line flags. Note that while the more common `--help` is also
available, it is not usable under Clojure CLI, instead it will print the help
information for Clojure itself.

Conceptually Kaocha goes through three steps: load configuration, load tests,
run tests. The result of each step is a data structure. You can view these
structures (EDN) with `--print-config`, `--print-test-plan`, and
`--print-result`.

``` clojure
--print-config                    Print out the fully merged and normalized config, then exit.
--print-test-plan                 Load tests, build up a test plan, then print out the test plan and exit.
--print-result                    Print the test result map as returned by the Kaocha API.
```

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
bin/kaocha --reporter kaocha.report.progress/report
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
can, e.g., reproduce a test run that failed on a build server.

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
occurs, use `--no-capture-output`.

Kaocha uses `deep-diff2` when tests fail to distinguish the difference between
the actual and expected values. If you don't like the format, or if it provides
unhelpful output in a particular scenario, you can turn it off using the
`--diff-style :none` option.

![Terminal screenshot showing an expected value of "{:expected-key 1}" and an actual value. ":unexpected-key 1" is in green because it is an extra key not expected and "expected-key 1" is in red because it was expected but not present.](./deep-diff.png)

## Parallelization

Kaocha allows you to run tests in parallel using the `:parallel` key or
`--parallel` flag. This is primarily useful for I/O heavy tests, but could also
be useful for CPU-bound tests.

Before enabling parallelization, be sure to test it. Consider using a tool like
`bench` or `hyperfine`. While Kaocha's built-in profiling tools are great for
identifying specific slow tests, but don't repeatedly measure your entire test suite
to account for variation and noise. If you want to test it on CI, test it for CI
specifically. CI runners are often lower powered than even a middle-of-the-road laptop.

`test.check` tests consist of repeatedly testing a property against random data.
In principle, these tests would be an excellent use case for parallelization.
Because this repeated testing happens within `test.check`, Kaocha sees it as a
single test. If you have many property-based tests that take a similar amount of
time, parallelization is a great fit. However, if you have one or two
property-based tests that take the bulk of the time, parallelization may not
make a significant difference because the work cannot be split up.

If you want to disable parallelization that's enabled in your configuration, you can
pass `--no-parallel`. If you find yourself frequently reaching for this flag,
it's probably worth reconsidering your configuration—having to frequently
disable parallelization might be negating any time saved by parallelization.

## Debug information

`--version` prints version information, whereas `--test-help` will print the
available command line flags. Note that while the more common `--help` is also
available, it is not usable under Clojure CLI, instead it will print the help
information for Clojure itself.

Conceptually Kaocha goes through three steps: load configuration, load tests,
and run tests. The result of each step is a data structure. You can view these
structures (EDN) with `--print-config`, `--print-test-plan`, and
`--print-result`.

``` clojure
--print-config                    Print out the fully merged and normalized config, then exit.
--print-test-plan                 Load tests, build up a test plan, then print out the test plan and exit.
--print-result                    Print the test result map as returned by the Kaocha API.
```

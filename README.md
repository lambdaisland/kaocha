# Kaocha

Full featured next gen Clojure test runner.

```
USAGE:

clj -m lambdaisland.kaocha.runner [OPTIONS]... [TEST-SUITE]...

  -c, --config-file FILE    tests.edn                            Config file to read.
      --print-config                                             Print out the fully merged and normalized config, then exit.
      --fail-fast                                                Stop testing after the first failure.
      --[no-]color                                               Enable/disable ANSI color codes in output. Defaults to true.
      --[no-]randomize                                           Run test namespaces and vars in random order.
      --seed SEED                                                Provide a seed to determine the random order of tests.
      --reporter SYMBOL     lambdaisland.kaocha.report/progress  Change the test reporter, can be specified multiple times.
      --test-path PATH      test                                 Path to scan for test namespaces.
      --ns-pattern PATTERN  -test$                               Regexp pattern to identify test namespaces.
  -H, --test-help                                                Display this help message.

Options may be repeated multiple times for a logical OR effect.
```

## Features

- Configure test suites per project, e.g. unit/functional/integration
- Handle classpath and loading of tests
- Fail fast mode: stop at first failure and print report
- Detect when interrupted with ctrl-C and print report
- Randomize order of tests to detect ordering dependencies
- Cloverage support
- API usage
- Custom, composable reporters

## Getting started

Add Kaocha to the project

``` clojure
;; deps.edn
{:aliases
 {:test
  :extra-deps {lambdaisland/kaocha {:mvn/version "VERSION"}}
  :main-opts ["-m" "lambdaisland.kaocha.runner"]}}
```

Create a stub `tests.edn`

```
clj -A:test --print-config >> tests.edn
```

Run tests

```
clj -A:test
```

## License

&copy; Arne Brasseur 2018
Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt

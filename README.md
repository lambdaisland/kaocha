# Kaocha [![CircleCI](https://circleci.com/gh/lambdaisland/kaocha.svg?style=svg)](https://circleci.com/gh/lambdaisland/kaocha)

Full featured next generation test runner for Clojure.

## Links

- [Github](https://github.com/lambdaisland/kaocha)
- [Clojars](https://clojars.org/lambdaisland/kaocha)
- [cljdoc](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT)

## Docs

<!-- docs-toc -->
- [1. Introduction](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/doc/01_introduction.md)
- [2. Installing](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/doc/02_installing.md)
- [3. Configuration](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/doc/03_configuration.md)
- [4. Running Kaocha CLI](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/doc/04_running_kaocha_cli.md)
- [4. Focusing and Skipping](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/doc/05_focusing_and_skipping.md)
- [6. Plugins](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/doc/06_plugins.md)
- [7. Extending](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/doc/07_extending.md)
<!-- /docs-toc -->

## Features

Features include

- Filtering tests based on test names or metadata
- Watch mode: watch the file system for changes and re-run tests
- Pretty, pluggable reporting
- Randomize test order
- Detect when interrupted with ctrl-C and print report
- Fail fast mode: stop at first failure and print report
- Profiling (show slowest tests)
- Dynamic classpath handling
- Tests as data (get test config, test plan, or test results as EDN)
- Extensible test types (clojure.test, Midje, ...)
- Extensible through plugins 
- Tool agnostic (Clojure CLI, Leiningen, ...)

## Quick start

This is no replacement for reading the docs, but if you're particularly
impatient to try it out, or if you already know Kaocha and need a quick
reference how to set up a new project, then this guide is for you.

Add Kaocha as a dependency, preferably under an alias.

``` clojure
;; deps.edn
{:deps { ,,, }
 :aliases
 {:test {:deps {lambdaisland/kaocha {:mvn/version "0.0-134"}}}}}
```

Add a wrapper/binstub

```
mkdir -p bin
echo '#!/bin/bash' > bin/kaocha
echo 'clojure -A:test -m kaocha.runner "$@"' >> bin/kaocha
chmod +x bin/kaocha
```

Add a `tests.edn` at the root of the project, add a first test suite with test
and source paths. Optionally set a reporter or load plugins.

``` clojure
#kaocha
{:tests [{:id :unit
          :test-paths ["test/unit"]
          :source-paths ["src"]}]
 ;; :reporter kaocha.report.progress/progress
 ;; :plugins [kaocha.plugin/profiling]
 }
```

Run your tests

``` shell
bin/kaocha

# Watch for changes
bin/kaocha --watch

# Exit at first failure
bin/kaocha --fail-fast

# Only run the `unit` suite
bin/kaocha unit

# Only run a single test
bin/kaocha --focus my.app.foo-test/bar-test

# Use an alternative config file
bin/kaocha --config-file tests_ci.edn

# See all available options
bin/kaocha --test-help
```

## License

&copy; Arne Brasseur 2018
Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt

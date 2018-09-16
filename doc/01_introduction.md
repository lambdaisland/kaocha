## 1. Introduction

> I test, therefore I am. — René Descartes

Kaocha is a _test runner_, its core task is to load tests and execute them,
reporting on their progress and final result.

It provides a uniform way for projects to define their test setup: the different
test suites they have, the testing libraries they use, the output reporting they
prefer. This way developers can jump into a new project and instantly be up to
speed.

Kaocha understands different types of tests: `clojure.test`, Midje, in the
future even ClojureScript, so that all of a project's tests can be handled in
the same way.

It aims to deliver all the features a developer might expect from their test
tooling. Different people have different workflows, different styles of writing
and running tests. We want to make sure we got you covered.

Much of this is achieved through plugins. This way the Kaocha core can remain
focused, while making it easy to experiment with new features.

Kaocha largely sprang from the desire to bring the experience found in other
language ecosystems to Clojure. If you came to Clojure from another language,
and you're missing some part of the test tooling you used to have, then please
file and issue and we'll try to sort you out.

To use Kaocha you create a `tests.edn` at the root of your project, and run
tests from the command line or from the REPL.

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

## Current status

Kaocha is a work in progress. Focus so far has been on internal APIs, and on the
data formats for configuration, test plan, and test results. These things are
largely stable and complete.

The command line test runner is largely feature complete, and being used in the
real world. More work is still needed to support and intergrate with other
frameworks and tools.

- [X] clojure.test support 
- [X] Midje support
- [-] ClojureScript support
- [ ] Expectation support
- [X] Clojure CLI
- [X] Leiningen
- [ ] Boot
- [ ] Cloverage

Currently Kaocha's versioning scheme is `0.0-${commit count}`, and releases are
made often. As long as the version is at `0.0` Kaocha will be considered alpha,
in other words: subject to change. Keep an eye on the CHANGELOG.

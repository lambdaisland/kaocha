## 1. Introduction

> Quality is not an act, it is a habit. — Aristotle

Kaocha is an all-in-one testing tool, its core task is to load tests and execute
them, reporting on their progress and final result. It does this in a way that
encourages good habits, supports multiple workflow styles, and optimizes for
ergonomics.

Kaocha has a modular architecture. It understands different types of tests:
`clojure.test`, ClojureScript, Cucumber, Fudje, Expectations, so that all of a
project's tests can be handled in the same way, and so that more can be added
without requiring changes to the core.

It aims to deliver all the features a developer might expect from their test
tooling. Different people have different workflows, different styles of writing
and running tests. We want to make sure we got you covered.

Much of this is achieved through plugins. This way the Kaocha core can remain
focused, while making it easy to experiment with new features.

To use Kaocha you create a `tests.edn` at the root of your project, and run
tests from the command line or from the REPL.

Features include:

- Filtering tests based on test names or metadata
- Watch mode: watch the file system for changes and re-run tests
- Pretty, pluggable reporting
- Randomize test order
- Detect when interrupted with Ctrl-C and print report
- Fail fast mode: stop at first failure and print report
- Profiling (show slowest tests)
- Dynamic classpath handling
- Tests as data (get test config, test plan, or test results as EDN)
- Extensible test types (clojure.test, Cucumber, ...)
- Extensible through plugins
- Tool agnostic (Clojure CLI, Leiningen, boot)

Currently Kaocha's versioning scheme is `1.${release count}-${commit count}`, and releases are
made often. Kaocha is stable, but we occasionally release breaking changes. We try to minimize 
the impact and the number of people affected, but it's still good to keep an eye on the CHANGELOG.

Kaocha requires Clojure 1.9. ClojureScript support requires Clojure and
ClojureScript 1.10.

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

- Watching for changes and re-running tests
- Filtering tests based on test names or metadata
- Pretty reporting
- Randomize test order
- Smart handling of Ctrl-C (show info on failed tests before exiting)
- Profiling (show slowest tests)
- Dynamic classpath handling
- Tests as data (get test config, test plan, or test results as EDN)

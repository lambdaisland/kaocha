# 01. Kaocha 考察

## Links

- [Github](https://github.com/lambdaisland/kaocha)
- [Clojars](https://clojars.org/lambdaisland/kaocha)
- [cljdoc](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT)

## Introduction

> I test, therefore I am. — René Descartes

Kaocha is a _test runner_, its core task is to load tests and execute them,
reporting on their progress and final result.

Kaocha provides a uniform way for projects to define their test setup: the
different test suites they have, the frameworks they use, the output reporting
they prefer.

Kaocha understands different types of tests: `clojure.test`, Midje, in the
future even ClojureScript, so that all of a project's tests can be handled in
the same way.

Kaocha aims to deliver all the features a developer might expect from their test
tooling. Different people have different workflows, different styles of writing
and running tests. We want to make sure we got you covered.

Much of this is achieved through plugins. This way the Kaocha core can remain
focused, while making it easy to experiment with new features.

Kaocha largely sprang from the desire to bring the experience found in other
language ecosystems to Clojure. If you came to Clojure from another language,
and you're missing some part of the test tooling you used to have, then please
file and issue and we'll try to sort you out.

<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# CLI: `--reporter` option

The progress and summary printed by Kaocha are done by one or more "reporter"
  functions. A reporter can be specified with the `--reporter` option followed
  by a fully qualified function name.

  Reporters in the `kaocha.report` namespace can be specified without a
  namespace prefix.

  See the
  [`kaocha.report`](https://github.com/lambdaisland/kaocha/blob/master/src/kaocha/report.clj)
  namespace for built-in reporters.

## Background: An example test

- <em>Given </em> a file named "test/my/project/reporter_test.clj" with:

``` clojure
(ns my.project.reporter-test
  (:require [clojure.test :refer :all]))

(deftest test-1
  (is (= 1 0)))

(deftest test-2
  (is true)
  (is (throw (Exception. "")))
  (is true))

(deftest test-3
  (is true))
```



## Using a fully qualified function as a reporter

- <em>When </em> I run `bin/kaocha --reporter kaocha.report/documentation`

- <em>And </em> the output should contain:

``` nil
my.project.reporter-test
  test-1 FAIL
  test-2 ERROR
  test-3
```



## Specifying a reporter via shorthand

- <em>When </em> I run `bin/kaocha --reporter documentation`

- <em>Then </em> the exit-code should be 2

- <em>And </em> the output should contain:

``` nil
my.project.reporter-test
  test-1 FAIL
  test-2 ERROR
  test-3
```



## Using a reporter which does not exist

- <em>When </em> I run `bin/kaocha --reporter does/not-exist`

- <em>Then </em> stderr should contain

``` nil
ERROR: Failed to resolve reporter var: does/not-exist
```




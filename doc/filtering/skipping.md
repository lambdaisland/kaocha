<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# Skipping test based on ids

You can tell Kaocha to completely ignore certain tests or test groups, either
  with the `--skip` command line flag, or the `:kaocha.filter/skip` test suite
  configuration key.

  Both of these take test ids or test group ids (e.g. the fully qualified name
  of a test var, or the name of a test namespace).

## Background: A simple test suite

- <em>Given </em> a file named "test/my/project/sample_test.clj" with:

``` clojure
(ns my.project.sample-test
  (:require [clojure.test :refer :all]))

(deftest some-test
  (is (= 1 1)))

(deftest other-test
  (is (= 2 2)))
```


- <em>And </em> a file named "test/my/project/other_sample_test.clj" with:

``` clojure
(ns my.project.other-sample-test
  (:require [clojure.test :refer :all]))

(deftest other-test
  (is (= 3 3)))
```



## Skipping test id from the command line

- <em>When </em> I run `bin/kaocha --skip my.project.sample-test/some-test --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.other-sample-test
  other-test

my.project.sample-test
  other-test

2 tests, 2 assertions, 0 failures.
```



## Skipping a test group id from the command line

- <em>When </em> I run `bin/kaocha --skip my.project.sample-test --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.other-sample-test
  other-test

1 tests, 1 assertions, 0 failures.
```



## Skipping via configuration

- <em>Given </em> a file named "tests.edn" with:

``` edn
#kaocha/v1
{:tests [{:kaocha.filter/skip [my.project.sample-test]}]
 :color? false
 :randomize? false}
```


- <em>When </em> I run `bin/kaocha --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.other-sample-test
  other-test

1 tests, 1 assertions, 0 failures.
```




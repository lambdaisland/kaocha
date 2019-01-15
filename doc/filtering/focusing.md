# Focusing on specific tests

You can limit the test run to only specific tests or test groups (e.g.
  namespaces) using the `--focus` command line flag, or `:kaocha.filter/focus`
  key in test suite configuration.

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
  (is (= 1 2)))
```



## Focusing on test id from the command line

- <em>When </em> I run `bin/kaocha --focus my.project.sample-test/some-test --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  some-test

1 tests, 1 assertions, 0 failures.
```



## Focusing on test group id from the command line

- <em>When </em> I run `bin/kaocha --focus my.project.sample-test --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  other-test
  some-test

2 tests, 2 assertions, 0 failures.
```



## Focusing via configuration

- <em>Given </em> a file named "tests.edn" with:

``` edn
#kaocha/v1
{:tests [{:kaocha.filter/focus [my.project.sample-test/other-test]}]
 :color? false
 :randomize? false}
```


- <em>When </em> I run `bin/kaocha --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  other-test

1 tests, 1 assertions, 0 failures.
```




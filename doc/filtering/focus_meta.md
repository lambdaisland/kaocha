<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# Focusing based on metadata

You can limit the test run based on test's metadata. How to associate metadata
  with a test depends on the test type, for `clojure.test` type tests metadata
  can be associated with a test var or test namespace.

  Using the `--focus-meta` command line flag, or `:kaocha.filter/focus-meta` key
  in test suite configuration, you can limit the tests being run to only those
  where the given metadata key has a truthy value associated with it.

## Background: Some tests with metadata

- <em>Given </em> a file named "test/my/project/sample_test.clj" with:

``` clojure
(ns ^:xxx my.project.sample-test
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

(deftest ^:yyy other-test
  (is (= 3 3)))
```



## Focusing by metadata from the command line

- <em>When </em> I run `bin/kaocha --focus-meta :xxx --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  other-test
  some-test

2 tests, 2 assertions, 0 failures.
```



## Focusing on a test group by metadata from the command line

- <em>When </em> I run `bin/kaocha --focus-meta :yyy --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.other-sample-test
  other-test

1 tests, 1 assertions, 0 failures.
```



## Focusing based on metadata via configuration

- <em>Given </em> a file named "tests.edn" with:

``` edn
#kaocha/v1
{:tests [{:kaocha.filter/focus-meta [:yyy]}]
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




<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# Skipping based on metadata

You can limit the test run based on test's metadata. How to associate metadata
  with a test depends on the test type, for `clojure.test` type tests metadata
  can be associated with a test var or test namespace.

  Using the `--skip-meta` command line flag, or `:kaocha.filter/skip-meta` key
  in test suite configuration, you can tell Kaocha to completely ignore those
  tests where the given metadata key has a truthy value associated with it.

  The default value for `:kaocha.filter/skip-meta` is `[:kaocha/skip]`, so you
  can use `^:kaocha/skip` to ignore tests without extra configuration.

## Skipping a test based on metadata from the command line

- <em>Given </em> a file named "test/my/project/sample_test.clj" with:

``` clojure
(ns my.project.sample-test
  (:require [clojure.test :refer :all]))

(deftest some-test
  (is (= 1 1)))

(deftest ^:xxx other-test
  (is (= 2 2)))
```


- <em>When </em> I run `bin/kaocha --skip-meta :xxx --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  some-test

1 tests, 1 assertions, 0 failures.
```



## Skipping a test group based on metadata from the command line

- <em>Given </em> a file named "test/my/project/sample_test.clj" with:

``` clojure
(ns my.project.sample-test
  (:require [clojure.test :refer :all]))

(deftest some-test
  (is (= 1 1)))
```


- <em>And </em> a file named "test/my/project/other_sample_test.clj" with:

``` clojure
(ns ^:xxx my.project.other-sample-test
  (:require [clojure.test :refer :all]))

(deftest other-test
  (is (= 1 1)))
```


- <em>When </em> I run `bin/kaocha --skip-meta :xxx --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  some-test

1 tests, 1 assertions, 0 failures.
```



## Skipping based on metadata via configuration

- <em>Given </em> a file named "tests.edn" with:

``` edn
#kaocha/v1
{:tests [{:kaocha.filter/skip-meta [:xxx]}]
 :color? false
 :randomize? false}
```


- <em>And </em> a file named "test/my/project/sample_test.clj" with:

``` clojure
(ns my.project.sample-test
  (:require [clojure.test :refer :all]))

(deftest some-test
  (is (= 1 1)))

(deftest ^:xxx other-test
  (is (= 2 2)))

(deftest ^:kaocha/skip also-skipped
  (is (= 3 3)))
```


- <em>When </em> I run `bin/kaocha --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  some-test

1 tests, 1 assertions, 0 failures.
```



## Skipping using the default `:kaocha/skip` metadata

- <em>Given </em> a file named "test/my/project/sample_test.clj" with:

``` clojure
(ns my.project.sample-test
  (:require [clojure.test :refer :all]))

(deftest some-test
  (is (= 1 1)))

(deftest ^:kaocha/skip other-test
  (is (= 2 2)))
```


- <em>When </em> I run `bin/kaocha --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  some-test

1 tests, 1 assertions, 0 failures.
```



## Replacing skip metadata

- <em>Given </em> a file named "tests.edn" with:

``` edn
#kaocha/v1
{:tests [{:kaocha.filter/skip-meta ^:replace [:xxx]}]
 :color? false
 :randomize? false}
```


- <em>Given </em> a file named "test/my/project/sample_test.clj" with:

``` clojure
(ns my.project.sample-test
  (:require [clojure.test :refer :all]))

(deftest ^:xxx some-test
  (is (= 1 1)))

(deftest ^:kaocha/skip other-test ;; this is ignored now
  (is (= 2 2)))
```


- <em>When </em> I run `bin/kaocha --reporter documentation`

- <em>Then </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.sample-test
  other-test

1 tests, 1 assertions, 0 failures.
```




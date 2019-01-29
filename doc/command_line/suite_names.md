# CLI: Selecting test suites

Each test suite has a unique id, given as a keyword in the test configuration.
  You can supply one or more of these ids on the command line to run only those
  test suites.

## Background: Given two test suites, `:aaa` and `:bbb`

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:tests [{:id :aaa
          :test-paths ["tests/aaa"]}
         {:id :bbb
          :test-paths ["tests/bbb"]}]}
```


- <em>And </em> a file named "tests/aaa/aaa_test.clj" with:

``` clojure
(ns aaa-test (:require [clojure.test :refer :all]))
(deftest foo-test (is true))
```


- <em>And </em> a file named "tests/bbb/bbb_test.clj" with:

``` clojure
(ns bbb-test (:require [clojure.test :refer :all]))
(deftest bbb-test (is true))
```



## Specifying a test suite on the command line

- <em>When </em> I run `bin/kaocha aaa --reporter documentation`

- <em>And </em> the output should contain:

``` nil
aaa-test
  foo-test
```


- <em>And </em> the output should not contain

``` nil
bbb-test
```



## Specifying a test suite using keyword syntax

- <em>When </em> I run `bin/kaocha :aaa --reporter documentation`

- <em>And </em> the output should contain:

``` nil
aaa-test
  foo-test
```


- <em>And </em> the output should not contain

``` nil
bbb-test
```



## Specifying an unkown suite

- <em>When </em> I run `bin/kaocha suite-name`

- <em>Then </em> the output should contain:

``` nil
No such suite: :suite-name, valid options: :aaa, :bbb.
```




<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# `clojure.test` assertion extensions

When running `clojure.test` based tests through Kaocha, some of the behavior
is a little different. Kaocha tries to detect certain scenarios that are
likely mistakes which make a test pass trivially, and turns them into errors
so you can investigate and see what's up.

Kaocha will also render failures differently, and provides extra multimethods
to influence how certain failures are presented.

## Detecting missing assertions

- <em>Given </em> a file named "test/sample_test.clj" with:

``` clojure
(ns sample-test
  (:require [clojure.test :refer :all]))

(deftest my-test
  (= 4 5))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` text
FAIL in sample-test/my-test (sample_test.clj:4)
Test ran without assertions. Did you forget an (is ...)?
```



## Detecting single argument `=`

- <em>Given </em> a file named "test/sample_test.clj" with:

``` clojure
(ns sample-test
  (:require [clojure.test :refer :all]))

(deftest my-test
  (is (= 4) 5))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` text
FAIL in sample-test/my-test (sample_test.clj:5)
Equality assertion expects 2 or more values to compare, but only 1 arguments given.
Expected:
  (= 4 arg2)
Actual:
  (= 4)
1 tests, 1 assertions, 1 failures.
```



## Pretty printed diffs

- <em>Given </em> a file named "test/sample_test.clj" with:

``` clojure
(ns sample-test
  (:require [clojure.test :refer :all]))

(defn my-fn []
  {:xxx [1 2 3]
   :blue :red
   "hello" {:world :!}})

(deftest my-test
  (is (= {:xxx [1 3 4]
          "hello" {:world :?}}
         {:xxx [1 2 3]
          :blue :red
          "hello" {:world :!}})))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` text
FAIL in sample-test/my-test (sample_test.clj:10)
Expected:
  {"hello" {:world :?}, :xxx [1 3 4]}
Actual:
  {"hello" {:world -:? +:!}, :xxx [1 +2 3 -4], +:blue :red}
1 tests, 1 assertions, 1 failures.
```




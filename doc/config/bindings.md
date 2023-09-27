<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# Configuration: Bindings

You can configure dynamic vars from `tests.edn`, these will be bound to the
  given values during the complete loading and running of the tests.

  Technically they are bound after the config step, so `config` hooks will not
  see the given values, while `pre-load` up to `post-run` will.

  The `:bindings` configuration key takes a map from var name to value.

  Some suggestions of things you can do with this:

  - `kaocha.stacktrace/*stacktrace-filters* []` disable filtering of
    stacktraces, showing all stack frames
  - `kaocha.stacktrace/*stacktrace-stop-list* []` disable the shortening
    of the stacktrace (by default stops printing when it sees "kaocha.ns")
  - `clojure.pprint/*print-right-margin* 120` Make pretty printing use longer
    line lengths
  - `clojure.test.check.clojure-test/*report-completion* false, clojure.test.check.clojure-test/*report-trials* false`
    Make test.check less noisy.

## Binding dynamic vars

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:bindings {my.foo-test/*a-var* 456}}
```


- <em>And </em> a file named "test/my/foo_test.clj" with:

``` clojure
(ns my.foo-test
  (:require [clojure.test :refer :all]))

(def ^:dynamic *a-var* 123)

(deftest var-test
  (is (= 456 *a-var*)))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
1 tests, 1 assertions, 0 failures.
```



## Stacktrace filtering

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:bindings {kaocha.stacktrace/*stacktrace-filters* ["clojure.core"]}}
```


- <em>And </em> a file named "test/my/erroring_test.clj" with:

``` clojure
(ns my.erroring-test
  (:require [clojure.test :refer :all]))

(deftest stacktrace-test
  (is (throw (java.lang.Exception.))))

```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
clojure.lang
```


- <em>And </em> the output should not contain

``` nil
clojure.core
```



## Stacktrace filtering turned off

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:bindings {kaocha.stacktrace/*stacktrace-filters* []}}
```


- <em>And </em> a file named "test/my/erroring_test.clj" with:

``` clojure
(ns my.erroring-test
  (:require [clojure.test :refer :all]))

(deftest stacktrace-test
  (is (throw (java.lang.Exception.))))

```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
clojure.core
```



## Stacktrace shortening

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:bindings {kaocha.stacktrace/*stacktrace-stop-list* ["kaocha.runner"]}}
```


- <em>And </em> a file named "test/my/erroring_test.clj" with:

``` clojure
(ns my.erroring-test
  (:require [clojure.test :refer :all]))

(deftest stacktrace-test
  (is (throw (java.lang.Exception.))))

```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
(Rest of stacktrace elided)
```


- <em>And </em> the output should not contain

``` nil
kaocha.runner$
```



## Disable stacktrace shortening

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:bindings {kaocha.stacktrace/*stacktrace-filters* []
            kaocha.stacktrace/*stacktrace-stop-list* []}}
```


- <em>And </em> a file named "test/my/erroring_test.clj" with:

``` clojure
(ns my.erroring-test
  (:require [clojure.test :refer :all]))

(deftest stacktrace-test
  (is (throw (java.lang.Exception.))))

```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
kaocha.runner$
```


- <em>And </em> the output should not contain

``` nil
(Rest of stacktrace elided)
```




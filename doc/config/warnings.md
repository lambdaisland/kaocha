<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# Configuration: Warnings

Kaocha will warn about common mistakes.

## No config

- <em>Given </em> a file named "test/my/foo_test.clj" with:

``` clojure
(ns my.foo-test
  (:require [clojure.test :refer :all]))

(deftest var-test
  (is (= 456 456)))
```


- <em>When </em> I run `bin/kaocha -c alt-tests.edn`

- <em>Then </em> stderr should contain:

``` nil
Did not load a configuration file and using the defaults.
```



## Warn about bad configuration

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:plugins notifier}
```


- <em>And </em> a file named "test/my/foo_test.clj" with:

``` clojure
(ns my.foo-test
  (:require [clojure.test :refer :all]))

(deftest var-test
  (is (= 456 456)))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> stderr should contain:

``` nil
Invalid configuration file:
```




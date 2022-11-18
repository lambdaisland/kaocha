<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# Syntax errors are preserved

Syntax errors should be passed along.

## Show output of failing test

- <em>Given </em> a file named "test/sample_test.clj" with:

``` clojure
(ns sample-test
  (:require [clojure.test :refer :all]))

stray-symbol

(deftest stdout-pass-test
  (is (= :same :same)))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
Exception: clojure.lang.Compiler$CompilerException
```




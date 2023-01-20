<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# CLI: `--profile` option

The `--profile KEYWORD` flags sets the profile that is used to read the
  `tests.edn` configuration file. By using the `#profile {}` tagged reader
  literal you can provide different configuration values for different
  scenarios.

  If the `CI` environment value is set to `"true"`, as is the case on most CI
  platforms, then the profile will default to `:ci`. Otherwise it defaults to
  `:default`.

## Specifying profile on the command line

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:reporter #profile {:ci kaocha.report/documentation
                     :default kaocha.report/dots}}
```


- <em>And </em> a file named "test/my/project/my_test.clj" with:

``` clojure
(ns my.project.my-test
  (:require [clojure.test :refer :all]))

(deftest test-1
  (is true))
```


- <em>When </em> I run `bin/kaocha --profile :ci`

- <em>And </em> the output should contain:

``` nil
--- unit (clojure.test) ---------------------------
my.project.my-test
  test-1
```




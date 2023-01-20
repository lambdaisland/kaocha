<!-- This document is generated based on a corresponding .feature file, do not edit directly -->

# Plugin: Capture output

Kaocha has a plugin which will capture all output written to stdout or stderr
  during the test run. When tests pass this output is hidden, when they fail the
  output is made visible to help understand the problem.

  This plugin is loaded by default, but can be disabled with `--no-capture-output`

## Show output of failing test

- <em>Given </em> a file named "test/sample_test.clj" with:

``` clojure
(ns sample-test
  (:require [clojure.test :refer :all]))

(deftest stdout-pass-test
  (println "You peng zi yuan fang lai")
  (is (= :same :same)))

(deftest stdout-fail-test
  (println "Bu yi le hu?")
  (is (= :same :not-same)))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
FAIL in sample-test/stdout-fail-test (sample_test.clj:10)
Expected:
  :same
Actual:
  -:same +:not-same
╭───── Test output ───────────────────────────────────────────────────────
│ Bu yi le hu?
╰─────────────────────────────────────────────────────────────────────────
2 tests, 2 assertions, 1 failures.
```



## Bypass output capturing

- <em>Given </em> a file named "test/sample_test.clj" with:

``` clojure
(ns sample-test
  (:require [clojure.test :refer :all]
            [kaocha.plugin.capture-output :as capture]))

(deftest stdout-pass-test
  (capture/bypass
    (println "You peng zi yuan fang lai"))
  (is (= :same :same)))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
[(You peng zi yuan fang lai
.)]
1 tests, 1 assertions, 0 failures.
```




# Plugin: Hooks

The hooks plugin allows hooking into Kaocha's process with arbitrary
  functions. This is very similar to using writing a plugin, but requires less
  boilerplate.

  See the documentation for extending Kaocha for a description of the different
  hooks. The supported hooks are: pre-load, post-load, pre-run, post-run,
  pre-test, post-test, pre-report.

  The hooks plugin also provides two extra hooks at the test suite level,
  `:kaocha.hooks/before` and `:kaocha.hooks/after`, which run respectively
  before any pre-test, and after any post-test hooks.

  Hooks can be specified as a fully qualified symbol, or a collection thereof.
  The referenced namespaces will be loaded during the config phase.

## Implementing a hook

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:plugins [:kaocha.plugin/hooks]
 :kaocha.hooks/pre-test [my.kaocha.hooks/sample-hook]}
```


- <em>And </em> a file named "src/my/kaocha/hooks.clj" with:

``` clojure
(ns my.kaocha.hooks)

(println "ok")

(defn sample-hook [test test-plan]
  (if (re-find #"fail" (str (:kaocha.testable/id test)))
    (assoc test :kaocha.testable/pending true)
    test))
```


- <em>And </em> a file named "test/sample_test.clj" with:

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
PENDING sample-test/stdout-fail-test (sample_test.clj:8)
```



## Implementing a test-suite specific hook

- <em>Given </em> a file named "tests.edn" with:

``` clojure
#kaocha/v1
{:plugins [:kaocha.plugin/hooks]
 :tests [{:id :unit
          :kaocha.hooks/before [my.kaocha.hooks/sample-before-hook]
          :kaocha.hooks/after  [my.kaocha.hooks/sample-after-hook]}]}
```


- <em>And </em> a file named "src/my/kaocha/hooks.clj" with:

``` clojure
(ns my.kaocha.hooks)

(defn sample-before-hook [suite test-plan]
  (println "before suite:" (:kaocha.testable/id suite))
  suite)

(defn sample-after-hook [suite test-plan]
  (println "after suite:" (:kaocha.testable/id suite))
  suite)
```


- <em>And </em> a file named "test/sample_test.clj" with:

``` clojure
(ns sample-test
  (:require [clojure.test :refer :all]))

(deftest stdout-pass-test
  (println "You peng zi yuan fang lai")
  (is (= :same :same)))
```


- <em>When </em> I run `bin/kaocha`

- <em>Then </em> the output should contain:

``` nil
before suite: :unit
[(.)]after suite: :unit
```




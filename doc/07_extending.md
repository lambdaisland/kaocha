# 7. Extending

Kaocha is designed to be extensible and customizable, so that it can adapt to
the needs of different projects, and so that it can act as a common base layer
upon which innovative or advanced testing features can be delivered.

## Concepts

The key to understanding Kaocha is understanding the three steps that Kaocha
goes through to perform a test run, and the three associated data structures.

### Run structure

A Kaocha test run consists of three part

- Configure
- Load
- Run

In the configure step the configuration file is loaded, normalized, and any
command line options merged in. Plugins are loaded at this staged, and given a
chance to update the configuration. The result is a Kaocha configuration (spec:
`:kaocha/config`)

After this each test suite loads. This means loading the test namespaces and
finding out which tests are in them. The loading specifics are delegated to the
test suite type. After the load step the Kaocha configuration has transformed
into a test plan (spec: `:kaocha/test-plan`), containing a nested collection of
"testables", providing a detailed overview of which tests will be run.

Finally the test-plan gets run, which means recursively executing the collection
of testables. Each testable gets updated with information about the test run:
whether it failed/passed/errored, captured output or exceptions, etc. After this
step the test-plan has transformed into a test result. (spec: `:kaocha/result`)

It's a good idea to keep the [specs](/src/kaocha/specs.clj) handy as a
reference.

### testable

A testable is a map containing the testable's type, id, type specific
information, and nested testables.

There are three versions of testables. In the configuration there are only
top-level testables, also called test suites, stored under `:kaocha/tests`.

``` clojure
{:kaocha/tests [{:kaocha.testable/type :kaocha.type/clojure.test
                 :kaocha.testable/id :unit
                 :kaocha/source-paths ["src"]
                 :kaocha/test-paths ["test"]}]}
```

Loading these tests is done with the `kaocha.testable/-load` multimethod, which
dispatches on the testable type.

After the load step these have become test-plan testables, with lots of extra
info, including nested test-plan testables. 

``` clojure
{:kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/clojure.test
                           :kaocha.testable/id :unit
                           :kaocha/source-paths ["src"]
                           :kaocha/test-paths ["test"]
                           :kaocha.testable/meta {}
                           :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/ns
                                                     :kaocha.testable/id :kaocha.runner-test
                                                     ,,,
                                                     :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.testable/var
                                                                               :kaocha.testable/id :kaocha.runner-test/main-test
                                                                               ,,,}]}]}]}
```

Running these tests is again type specific, each type has an implementation of
`kaocha.testable/-run`, which recursively calls `kaocha.testable/run-tests`,
which collects the results into the test result data structure.

``` clojure
{:kaocha.result/tests [{:kaocha.testable/type :kaocha.type/clojure.test
                        :kaocha.testable/id :unit
                        :kaocha/source-paths ["src"]
                        :kaocha/test-paths ["test"]
                        :kaocha.testable/meta {}
                        :kaocha.result/tests [{:kaocha.testable/type :kaocha.type/ns
                                               :kaocha.testable/id :kaocha.runner-test
                                               ,,,
                                               :kaocha.result/tests [{:kaocha.testable/type :kaocha.testable/var
                                                                      :kaocha.testable/id :kaocha.runner-test/main-test
                                                                      :kaocha.result/count 1
                                                                      :kaocha.result/pass 1
                                                                      :kaocha.result/fail 0
                                                                      :kaocha.result/error 0
                                                                      ,,,}]}]}]}
```

## Extension types

### Plugins

A plugin consists of functions that get run when certain "hooks" within Kaocha
fire, bundled in a map from keyword to function.

To write a Kaocha plugin you implement the `kaocha.plugin/-register`
multimethod. This allows the plugin to add itself to the "plugin chain", a
vector of plugin maps.

``` clojure
(ns my.kaocha.plugin
  (:require [kaocha.plugin :as p]))
  
(defmethod p/-register :my.kaocha/plugin [_name plugins]
  (conj plugins
        {:kaocha.hooks/config
         (fn [config]
           (assoc config ::setting :foo))

         :kaocha.hooks/pre-run
         (fn [test-plan]
           (println "run is starting!")
           test-plan)]]}))
```
 
Plugin names must be namespaced keywords. If your plugin is called
`:foo.bar/baz` then it must be implemented in the namespace `foo.bar` or
`foo.bar.baz`. This will allow Kaocha to automatically load the plugin before
calling it.

To take the boilerplate out of writing plugins you are encouraged to use the
`defplugin` macro.

These are all the hooks a plugin can implement. Note that each must return its
first argument (possibly updated).

``` clojure
(ns my.kaocha.plugin
  (:require [kaocha.plugin :as p]))

(p/defplugin my.kaocha/plugin
  ;; Install extra CLI options and flags.
  (cli-options [opts]
    opts)
  
  ;; Alter the configuration. Useful for setting default values.
  (config [config]
    config)
    
  ;; Runs before the load step
  (pre-load [config]
    config)

  ;; Runs after the load step
  (post-load [test-plan]
    test-plan)
    
  ;; Runs before the run step
  (pre-run [test-plan]
    test-plan)
    
  ;; Runs before each individual test
  (pre-test [test test-plan]
    test)
    
  ;; Runs after each individual test
  (post-test [test test-plan]
    test)
    
  ;; Runs after the run step
  (post-run [result]
    result)
    
  ;; Allows "wrapping" the run function
  (wrap-run [run test-plan]
    run))
```

### Suite types

Kaocha is designed to be a universal tool, able to run any type of test suite
your project chooses to use. To make this possible it provides a way to
implement custom test suite types.

In the test configuration every suite has a type.

``` clojure
{:kaocha/tests [{:kaocha.testable/type :kaocha.type/clojure.test
                 :kaocha.testable/id   :unit}]}
```

When Kaocha encounters this test suite it will first try to load the type, by
requiring either the `kaocha.type` or `kaocha.type.clojure.test` namespace.

It will then validate the suite configuration using the
`:kaocha.type/clojure.test` spec, so a custom test suite implementation must
register a Clojure spec with the same name as the suite type.

Finally a test suite implements two multimethods, one that handles Kaocha's load stage, and one that handles the run stage.

Here's a skeleton example of a test suite. 

``` clojure
(ns kaocha.type.clojure.test
  (:require [clojure.spec.alpha :as s]
            [kaocha.testable :as testable]
            [kaocha.load :as load]
            [clojure.test :as t]))

(defmethod testable/-load :kaocha.type/clojure.test [testable]
  (assoc :kaocha.testable test-plan/tests (load-tests ...)))

(defmethod testable/-run :kaocha.type/clojure.test [testable test-plan]
  (t/do-report {:type :begin-test-suite})
  (let [results (testable/run-testables (:kaocha.test-plan/tests testable) test-plan)
        testable (-> testable
                     (dissoc :kaocha.test-plan/tests)
                     (assoc :kaocha.result/tests results))]
    (t/do-report {:type :end-test-suite
                  :kaocha/testable testable})
    testable))

(s/def :kaocha.type/clojure.test (s/keys :req [:kaocha/source-paths
                                               :kaocha/test-paths
                                               :kaocha/ns-patterns]))
```

Some things to note:

- you should structure your test types hierarchically, and use
  `kaocha.testable/load-testables` / `kaocha.testable/run-testables` to perform
  the recursion.
- the `-run` implementation is responsible for calling `clojure.test/do-report`
- `-load` transforms a config into a test-plan, so it should `dissoc
  :kaocha/tests` and `assoc :kaocha.test-plan/tests`
- `-run` transforms a test-plan into a test result, so it should `dissoc
  :kaocha.test-plan/tests`, and `assoc :kaocha.result/tests`.
- `-load` is responsible for adding the test directories to the classpath (if
  this applies for your test type). The helpers in `kaocha.load` will come in
  handy for this.
- When in doubt study the existing implementations.
  

### Reporters

Reporters generate the test runners output. They are in their nature side-effectful, printing to stdout in response to events. 

A reporter is a function which takes a single map as argument, with the map having a `:type` key. Kaocha uses the same types as `clojure.test`, but adds `:begin-test-suite` and `:end-test-suite`.

Kaocha contains fine-grained reporters, which you can combine, or mix with your own to get the desired output. A reporter can be either a function, or a sequence of reporters, which will all be called in turn. For instance, the default Kaocha reporter is defined as such:

``` clojure
(ns kaocha.report)

(def dots
  "Reporter that prints progress as a sequence of dots and letters."
  [dots* result])
```

Other reporters currently implemented include

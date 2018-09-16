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

### Reporters

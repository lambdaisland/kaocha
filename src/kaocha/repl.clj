(clojure.core/require 'kaocha.version-check)
(ns ^{:author "Arne Brasseur"
      :doc "REPL interface to Kaocha

## Running tests

To run tests from the REPL, use [[run]]. Without any arguments it runs all
tests in the current namespace. This is equivalent to `(run *ns*)`

``` clojure
(use 'kaocha.repl)

(run)
;;=> #:kaocha.result{:count 18, :pass 50, :error 0, :fail 0}
```

Pass one or more arguments to [[run]] to only run specific tests. This way
you can test a single var, a namespace, or a test suite. You can using keywords,
symbols, namespace objects, and vars.

``` clojure
(run :unit)                               ;; run the :unit test suite
(run 'kaocha.random-test)                 ;; run all tests in the kaocha.random-test namespace
(run 'kaocha.random-test/rand-ints-test)  ;; run the specified test
(run #'rand-ints-test)                    ;; test the given var
```

You can pass in any number of things to test. As a final argument you can pass
in a map to override specific configuration options. See [[config]] for syntax.

``` clojure
(run :foo.bar
     :bar.baz
     {:config-file \"my_tests.edn\"
      :focus-meta [:xxx]}) ;; run all tests with ^:xxx metadata
```

`run` always performs a full kaocha run, meaning all fixtures, plugins etc
will run.

Note that `deftest` returns the var it defines. This means that with in-buffer
evaluation you can use this pattern to quickly define and validate a test.

``` clojure
(run
  (defmethod my-test ,,,))
;;=> #:kaocha/result{:count 1, :pass 3, :error 0, :fail 0}
```

To run all tests defined in `tests.edn`, use [[run-all]]

## Inspecting configuration and test plan

Before running tests Kaocha builds up a configuration, and based on that loads
all tests and builds up a test-plan. If Kaocha is not behaving as you would
expect then inspecting the configuration and test-plan is a good way to figure
out what's going on. The [[config]] and [[test-plan]] functions provide this
kind of debugging information.

These will particularly come in handy when developing plugins."}
    kaocha.repl
  (:require [kaocha.config :as config]
            [kaocha.plugin :as plugin]
            [kaocha.api :as api]
            [kaocha.result :as result]
            [kaocha.output :as output]
            [clojure.java.io :as io]
            [slingshot.slingshot :refer [throw+ try+]]))

(defn config
  "Load the Kaocha configuration

  Optionally takes a map of extra configuration options. These can either use
  their full form (`:kaocha.filter/focus`), or the shortened form that is also
  available through the `#kaocha/v1` reader literal. See the Configuration
  chapter in the docs for more information.

  By default this uses `tests.edn` found in the project root. You can specify an
  alternative configuration file to use with `:config-file`."
  ([]
   (config {}))
  ([extra-config]
   (let [config-file (:config-file extra-config "tests.edn")
         config      (config/load-config2 config-file)
         plugin-chain (plugin/load-all (:kaocha/plugins config))]
     (plugin/with-plugins plugin-chain
       (plugin/run-hook :kaocha.hooks/config config)))))

(defn test-plan
  "Load tests and return the test plan

  This loads the configuration as per [[config]], then goes through Kaocha's
  load step and returns the test plan."
  ([]
   (test-plan {}))
  ([extra-config]
   (let [config       (config extra-config)
         plugin-chain (plugin/load-all (:kaocha/plugins config))]
     (plugin/with-plugins plugin-chain
       (api/test-plan config)))))

(defprotocol TestableId
  (testable-id [x]))

(extend-protocol TestableId
  clojure.lang.Keyword
  (testable-id [k] k)

  clojure.lang.Symbol
  (testable-id [k] (keyword k))

  java.lang.String
  (testable-id [s] (keyword s))

  clojure.lang.Namespace
  (testable-id [n] (keyword (str n)))

  clojure.lang.Var
  (testable-id [v]
    (keyword (str (:ns (meta v)))
             (str (:name (meta v))))))

(defn run
  "Run tests, returning a summary

  Arguments are things to test, any testable id can be specified, including test
  suite ids, namespace names, and fully qualified var names. Namespace and var
  objects can also be passed in directly.

  With zero arguments it tests the current `*ns*`.

  If the final argument is a map then it is used to build up the test
  configuration, see [[config]].

  This runs through a full Kaocha test run, so all fixtures and plugins are
  invoked, even when running a single test. Any active reporters will generate
  their output on stdout.

  Returns a summary map:

  ``` clojure
  (run) ;;=> #:kaocha.result{:count 18, :pass 50, :error 0, :fail 0}
  ```"
  ([]
   (run *ns*))
  ([& args]
   (let [[config-opts tests] (if (map? (last args))
                               [(last args) (or (butlast args) [*ns*])]
                               [{} args])
         config (-> (config config-opts)
                    (update :kaocha.filter/focus into (map testable-id) tests))]
     (try+
      (->> config
           api/run
           (plugin/run-hook :kaocha.hooks/post-summary)
           :kaocha.result/tests
           result/totals)
      (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
        (if (not= exit-code 0)
          (output/error "Test run exited with code " exit-code)
          (output/warn "Test run exited with code " exit-code))
        exit-code)))))

(defn run-all
  "Do a full Kaocha test run

  Run all tests as specified in `tests.edn`. Optionally takes a flag of extra
  configuration options, see [[config]] for details."
  ([]
   (run-all {}))
  ([extra-opts]
   (try+
    (result/totals (:kaocha.result/tests (api/run (config extra-opts))))
    (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
      (if (not= exit-code 0)
        (output/error "Test run exited with code " exit-code)
        (output/warn "Test run exited with code " exit-code))
      exit-code))))

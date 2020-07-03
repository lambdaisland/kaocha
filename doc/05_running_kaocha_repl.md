# 5. Running Kaocha From the REPL

For REPL use there's the
[kaocha.repl](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/api/kaocha.repl)
namespace. Its main entry point is the
[run](https://cljdoc.xyz/d/lambdaisland/kaocha/CURRENT/api/kaocha.repl#run)
function. Calling it is similar to starting Kaocha from the CLI, it will load
`tests.edn`, merge in any extra options and flags, and then load and run your
test suites.

## Filtering tests

As arguments to `run` you pass it one or more identifiers of things you want to
test. This can be a test suite, a namespace, or a specific test var.

Say you have a single `:unit` test suite, with a `kaocha.random-test` namespace
containing two tests.

```
.
└── :unit
    └── kaocha.random-test
        ├── kaocha.random-test/rand-ints-test
        └── kaocha.random-test/randomize-test

```

You could run the whole suite

``` clojure
(use 'kaocha.repl)

(run :unit)
```

The namespace

``` clojure
(run 'kaocha.random-test)
```

Or specific test vars

``` clojure
(run 'kaocha.random-test/rand-ints-test 'kaocha.random-test/randomize-test)
```

These are equivalent to using `--focus` on the command line. `run` also
understand namespace and var objects.


``` clojure
(run *ns*)
(run #'rand-ints-test)
```

`(run)` without any arguments is equivalent to `(run *ns*)`. If you really want to run all test suites without discrimination, use [run-all](https://cljdoc.org/d/lambdaisland/kaocha/CURRENT/api/kaocha.repl#run-all).


## Passing configuration

If the last argument to `(run)` is a map, then it is considered extra
configuration which is applied on top of what is read from `tests.edn`. The
special key `:config-file` is available to change the location from which
`tests.edn` is read.

``` clojure
(run {:config-file "/tmp/my_tests.edn"})
```

Other keys in the map need to be either fully qualified keywords as used in
Kaocha's configuration, or the short equivalent that is available in `tests.edn`
when using the `#kaocha/v1` reader tag.

## In-buffer eval

`kaocha.repl` is especially useful when used with a editor-connected REPL, so
that code can be evaluated in place. When working on a specific test you can
wrap it in `kaocha.repl/run`. Since `deftest` returns the var it defines, this
redefines and runs the test in one go.

``` clojure
(kaocha.repl/run
  (deftest my-test
    ,,,))
```

When using CIDER this combines really well with
`cider-pprint-eval-defun-at-point` (binding in CIDER 1.18.1: `C-c C-f`).

## Config and Test plan

The `(kaocha.repl/config)` and `(kaocha.repl/test-plan)` functions are very
useful when diagnosing issues, and can be helpful when developing plugins or
test types.

## Live reload at the REPL

To enable live reloading of tests in your REPL session, you can call 
`(kaocha.watch/run (kaocha.repl/config))`.

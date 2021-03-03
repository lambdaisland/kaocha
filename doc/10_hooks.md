# 10. Hooks

Kaocha aims to be flexible and adaptable, so that you are able to do things with
it that the authors did not anticipate. We achieve this goal by being
data-driven and by providing hooks. Data-driven means that Kaocha's behavior is
determined by plain data structures. By manipulating these data structures you
can change how Kaocha behaves.

With hooks you supply a function which gets called at a specific point within
Kaocha's process, the function gets passed some data structure, typically a
`config`, `test-plan`, or `testable`, and can then return an updated version of
that data structure, which Kaocha will then continue using. The hook function
can also simply perform side effects, and return the data structure unchanged.

There are two ways to provide Kaocha with these hook functions, you can write a
plugin, or you can write a plain function and use the hooks plugin to hook it
up. Both approaches are equivalent, everything you can do with a plugin you can
do through hooks. A plugin is nothing more than a collection of hook functions.

Plugins are great when you want to implement functionality and share it as a
reusable bundle, or if you are doing something that requires hooking into
multiple extension points at once. For simple project level customization of
Kaocha using hook functions and the hooks plugin provides an easier alternative.

This chapter will focus on the latter, project-level customization using the
hooks plugin. In [Chapter 9](09_extending.md) we talked about the data
structures that Kaocha uses, and about implementing plugins. It provides some
important background since to use hooks well you need to have some understanding
of how Kaocha's process works. It's a good idea to read through at least the
"Concepts" section, and perhaps refer to it later on.

**Note that hooks and plugins are not the only way to extend Kaocha. If you want to
customize how Kaocha reports test run events on the command line then the
recommended way to do this is through a custom reporter.**

To start using hooks first enable the hooks plugin.

``` clojure
#kaocha/v1
{:plugins [:hooks]}
```

Then add your first hook:

``` clojure
#kaocha/v1
{:plugins [:hooks]
 :kaocha.hooks/pre-load [myproject.kaocha-hooks/start-load-message]}
```

``` clojure
(ns myproject.kaocha-hooks)

(defn start-load-message [config]
  (println "About to start loading!")
  config)
```

In `tests.edn` you provide the fully qualified symbol name, and Kaocha will load
the namespace and use the function as a hook.

This function will be called at the start of the load process. Notice how it
returns its argument. This is very important, so that Kaocha still has something
to work with after your hook is finished. As a rule hooks should always return
(a possibly updated version of) their first argument.

## Config / Test-plan hooks

At a high level a Kaocha run consists of two main phases, `load` and `run`, and so you get four corresponding hooks:

- `pre-load`
- `post-load`
- `pre-run`
- `post-run`

`pre-load` receives a config (an expanded version of what is in your
`tests.edn`), `post-load` and `pre-run` receive a `test-plan`. This is like the
config, but it also contains a nested tree data structure representing all the
testables Kaocha found and is about to run.

`post-run` receives a `test-result`, which is like the test plan, but for each
testable information got added about the result of the test.

There's also a `config` hook, which runs even earlier than `pre-load`. Here you
can change config settings like `:kaocha/fail-fast?`, `:kaocha/color?` or
`:kaocha/bindings`.

## pre-test / post-test

`pre-test` and `post-test` run before/after each individual testable. Note that
a "testable" in Kaocha can be an individual test (a test var in clojure.test),
but also a collection of tests (like a test ns), or a full test suite.
`pre-test` and `post-test` will be run for each of these.

As arguments these receive the testable in question, and the test-plan.

So say you have a single unit test suite, with two namespaces, and each has a
single test var, then these hooks will be called in this order:

- pre-test unit
  - pre-test ns-1
    - pre-test var-1
    - post-test var-1
  - pre-test ns-2
    - pre-test var-2
    - post-test var-2
  - post-test ns-2
- post-test unit

You should use the helpers in the `kaocha.hierarchy` namespace to determine if a
testable is a suite (top-level), group (intermediate grouping), or leaf (an
individual test with assertions).

For example, a common use case of pre-test hooks is to skip certian tests based
on certain conditions. Here's an example hook that skips all tests that have
"foo" in their name, when running on CI.

``` clojure
(ns myproject.kaocha-hooks
  (require [kaocha.hierarchy :as hierarchy]))

(defn my-pre-test-hook [testable test-plan]
  (if (and (hierarchy/leaf? testable)
           (System/getenv "CI")
           (.contains (str (:kaocha.testable/id testabe)) "foo"))
    (assoc testable :kaocha.testable/skip true)
    testable))
```

``` clojure
#kaocha/v1
{:plugins [:hooks]
 :kaocha.plugin/hooks [myproject.kaocha-hooks/my-pre-test-hook]}
```

## pre-load-test / post-load-test

The `pre-load-test` / `post-load-test` hooks are called before and after each
individual test (testable) gets loaded. They are called with the testable and a
`config`.

Like `pre-test` / `post-test` it is up to you to filter out the testables you
are interested in, possibly using `kaocha.hierarchy`.

These hooks will fire for each test suite, and should fire for each group and
leaf testable, but this relies on the test suite type implementation yielding
control back to Kaocha when loading each level of tests, which may not always be
true.

## Suite level hooks

Generally hooks are declared at the top level of your test configuration
(`tests.edn`), but the testable-level hooks can also be declared on a test
suite. These are `pre-load-test`, `post-load-test`, `pre-test`, `post-test`.
When declared like this they will be called before/after that specific suite
gets loaded or run.

``` clojure
#kaocha/v1
{:tests [{:kaocha/pre-load-test [...]}]}
```

These can be used for instance with kaocha-cljs2 to prepare the ClojureScript
compilation and JavaScript runtime.

## Special purpose hooks

### wrap-run

Most of Kaocha's hooks have `pre-` and `post-` variants, but we don't have
"around" hooks. This can be annoying, e.g., when setting up dynamic bindings on a
per-test level, in this case you can use `wrap-run` to "wrap" Kaocha's run
function.

In particular this wraps `kaocha.testable/-run`, so it receives a two argument function (the arguments are testable and test-plan), and should return such a function. `wrap-run` also receives the test-plan directly.

``` clojure
(defn my-wrap-run-hook [run _test-plan]
  (fn [testable test-plan]
    (println "about to run" (:kaocha.testable/id testable))
    (run testable test-plan)))
```

`wrap-run` is used by the output capturing plugin (on by default) to rebind
`*out*` and `*err*` during tests.

### Pre-report

Kaocha reporters are based on Clojure.test reporters. They are functions that
receive "events" (map with a `:type` key), and based on those they print out
information to the terminal.

The `pre-report` hook allows you to modify these events before they are passed
to the reporter. They receive a single argument, the event map.

An example use case is the [kaocha-noyoda
plugin](https://github.com/magnars/kaocha-noyoda), which changes the assertion
order from `(= actual expected)` to `(= expected actual)`.

### Post-summary

This is the final hook that gets called, it runs after the finally summary has
been printed (after the `:summary` event has fired).

```
107 tests, 265 assertions, 0 failures.
```

The profiling plugin uses this hook to print out a list of the slowest tests.

Note that this hook only runs when Kaocha is invoked through `kaocha.runner`, so
through the command line. It does not get run when invoked via `kaocha.repl` or
`kaocha.api`.

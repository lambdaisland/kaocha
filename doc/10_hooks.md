# 10. Hooks

Kaocha aims to be flexible and adaptable, so that you are able to do things with
it that the authors did not anticipate. We achieve this goal by being
data-driven, and by providing hooks. Data-driven means that Kaocha's behavior is
determined by plain data structures. By changing these data structures you can
change how Kaocha behaves.

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
important background since to use hooks well you need to understand a bit how
Kaocha's process works. It's a good idea to read through at least the "Concepts**
section, and perhaps refer to it later on.

**Note that hooks/plugins are not the only way to extend Kaocha. If you want to
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

## Suite level hooks

Generally hooks are always declared at the top level of your test configuration
(`tests.edn`), but two hooks can also be declared on a test suite. These are
`pre-load` and `post-load`. When declared like this they will be called
before/after that specific suite gets loaded. Their arity is different from the
regular `pre-load/post-load`, they receive two arguments, the testable (test
suite), and the config.

These were added to support kaocha-cljs2, so that it's possible to add hooks
that perform ClojureScript compilation and set up a JS runtime before
continuing.

## Special purpose hooks

### wrap-run

Most of Kaocha's hooks have `pre-` and `post-` variants, but we don't have
"around" hooks. This can be annoying e.g. when setting up dynamic bindings on a
per-test level, in this case you can use `wrap-run` to "wrap" Kaocha's run
function.

In particular this wraps `kaocha.testable/-run`, so it receives a two argument function (the arguments are testable and test-plan), and should return such a function. `wrap-run` also receives the test-plan directly.

``` clojure
(defn my-wrap-run-hook [run _test-plan]
  (fn [testable test-plan]
    (println "about to run" (:kaocha.testable/id testable))
    (run testable test-plan)))
```

### Pre-report

Kaocha reporters are based on Clojure.test reporters, they are functions that
receive "events" (map with a `:type` key), and based on those they print out
information to the terminal.

The `pre-report` hook allows you to modify these events before they are passed
to the reporter. They receive a single argument, the event map.

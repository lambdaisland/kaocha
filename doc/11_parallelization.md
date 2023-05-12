# 11. Parallelization

Parallelization is an optional Kaocha feature, where it distributes your test
workload across multiple threads, to make better use of multiple CPU cores.

This is still a relatively new feature, one that has a chance of interfering in
various ways with plugins, custom hooks, or the particulars of test setups that
people have "in the wild". We very much welcome feedback and improvements.
Please be mindful and respectful of the maintainers though and report issues
clearly, preferably with a link to a git repository containing a minimal
reproduction of the issue.

## Configuration and high-level behavior

You can enable parallelization either via the `--parallelize` command line flag,
or by setting `:parallelize? true` in your `tests.edn`. This is assuming that
you are using the `#kaocha/v1` reader literal to provide normalization. The
canonical configuration key is `:kaocha/parallelize?`.

Kaocha looks at your tests as a hierarchy, at the top level there are your test
suites (e.g. unit vs intergration, or clj vs cljs). These contain groups of
tests (their children), e.g. one for each namespace, and these in turn contain
multiple tests, e.g. one for each test var.

Setting `:parallelize true?` at the top-level configuration, or using the
command line flag, will run any suites you have in parallel, as well making
parallelization the default for any type of testable that has children. So say
for instance you have a suite of type `clojure.test`, then multiple test
namespaces will be run in parallel, and individual test vars within those
namespaces will also be started in parallel.

Test type implementations need to opt-in to parallelization. For instance,
Clojure is multi-threaded, but ClojureScript (running on a JavaScript runtime)
is not, so thre is little benefit in trying to parallelize ClojureScript tests.
So even with parallelization on, `kaocha.cljs` or `kaocha.cljs2` tests will
still run in series.

## Fine-grained opting in or out

Using the command line flag or setting `:parallelize? true` at the top-level of
tests.edn will cause any testable that is parallelizable to be run in parallel.
If you want more fine-grained control you can configure specific test suites to
be parallelized, or set metadata on namespaces to opt in or out of
parallelization.

```clj
#kaocha/v1
{:tests [{:id :unit, :parallelize? true}])
```

This will cause all namespaces in the unit test suite to be run in parallel, but
since the default (top-level config) is not set, vars within those namespaces
will not run in parallel. But you can again opt-in at that specific level,
through metadata.

```clj
(ns ^{:kaocha/parallelize? true} my-test
 (:require [clojure.test :as t]))
  
...
```

Conversely you can opt-out of parallelization on the test suite or test
namespace level by setting `:kaocha/parallelize? false`.

## Caveats

When you start running your tests in parallel you will likely notice one or two
things. The first is that your output looks all messed up. Before you might see
something like `[(....)(.......)][(.......)]`, whereas now it looks more like
`[[(..(..).....(..)...]....)]`. This will be even more pronounced if you are for
instance using the documentation reporter. Output from multiple tests gets
interleaved, causing a garbled mess.

The default dots reporter is probably the most usable reporter right now.

The second thing you might notice is that you are getting failures where before
you got none. This likely indicates that your tests themselves are not thread
safe. They may for instance be dealing with shared mutable state.

You will have to examine your code carefully. Starting out with a more piecemeal
opting in might be helpful to narrow things down.

It is also possible that you encounters failures caused by Kaocha itself. In
that case please report them on our issue tracker.

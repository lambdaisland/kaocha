# 6. Focusing and Skipping

Often you will want to *skip* certain tests, so they don't get run, or you want
to *focus* on specific tests, so only those get run.

For example:

- Skip tests that aren't finished yet
- Skip tests marked as slow
- Focus on a test that previously failed

You can skip tests, or focus on tests, either based on the test ID, or on test
or namespace metadata, based on four command line flags and configuration keys.

``` shell
--skip SYM                        Skip tests with this ID and their children.
--focus SYM                       Only run this test, skip others.
--skip-meta SYM                   Skip tests where this metadata key is truthy.
--focus-meta SYM                  Only run tests where this metadata key is truthy.
```

``` clojure
#kaocha/v1
{:tests [{:id :unit
          :skip [...]
          :focus [...]
          :skip-meta [...]
          :focus-meta [...]}]}
```

## Matching

### On id

A test id is a namespaced symbol, for clojure.test tests this is the
fully qualified name of the test var. You can skip or focus on such a test
either by providing its full name, or just the namespace part.

So you can run a single test with

``` shell
bin/kaocha --focus com.my.project-test/foo-test
```

To run all tests in that namespace, use

``` shell
bin/kaocha --focus com.my.project-test
```

### On metadata

Suppose you have test that are checked into source code, but that still need
work. You can mark these with a metadata tag:

``` clojure
(deftest ^:pending my-test
  ,,,)
```

To ignore such tests, add a `:skip-meta` key to the test suite config:

``` clojure
#kaocha/v1 {:tests [{:id :unit
                  :skip-meta [:pending]}]}
```

This also works for metadata placed on the test's namespace.

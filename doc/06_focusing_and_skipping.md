# 6. Focusing and Skipping

Often you will want to *skip* certain tests, so they don't get run, or you want
to *focus* on specific tests, so only those get run.

For example, you can:

- Skip tests that aren't finished yet
- Skip tests marked as slow
- Focus on a test that previously failed

You can skip tests, or focus on tests, either based on a test `ID`, or on test
or namespace metadata, based on four command line flags and configuration keys.

``` shell
--skip SYM-OR-KW                  Skip tests with this ID and their children.
--focus SYM-OR-KW                 Only run this test, skip others.
--skip-meta SYM-OR-KW             Skip tests where this metadata key is truthy.
--focus-meta SYM-OR-KW            Only run tests where this metadata key is truthy.
```

## Matching

Before running Kaocha builds a test plan where all tests are
identified by a test `ID` keyword.  The command line then
canonicalises any IDs you supply into this keyword form, before
matching them for focussing or skipping.

### On a test suite

Assuming you have a test suite `:unit` specified in `tests.edn`:

``` clojure
#kaocha/v1
{:tests [{:id :unit
          :skip [...]
          :focus [...]
          :skip-meta [...]
          :focus-meta [...]}]}
```

You can focus on this by running:

``` shell
bin/kaocha --focus :unit
```

### On a namespace

If you have tests in a namespace `com.my.project-test` and you want to
run them all you can focus on them with the command:

``` shell
bin/kaocha --focus com.my.project-test
```

### On a test var

Assuming you have a test var defined with for example `clojure.test`
`deftest`, you can focus on it by supplying its fully qualified name
like so:

``` shell
bin/kaocha --focus com.my.project-test/foo-test
```

### On metadata

Suppose you have tests that are checked into version control, but that still need
work. You can mark these with a metadata tag:

``` clojure
(deftest ^:pending my-test
  ,,,)
```

To ignore such tests, add a `:skip-meta` key to the test suite config:

``` clojure
#kaocha/v1
{:tests [{:id :unit
          :skip-meta [:pending]}]}
```

And then run via the command:

``` shell
bin/kaocha --focus :unit
```

This also works for metadata placed in the test's namespace, or any other
metadata that a given test type implementation exposes. For example,
kaocha-cucumber converts scenario tags into metadata.

### Focusing on metadata: special case

`--focus-meta` will only work if at least one test has this metadata tag. If not
a single test matches then this metadata is ignored. Assuming no other filters
are in effect, this will result in running all tests.

This way you can configure a certain key in `tests.edn` that you can use when
you want to zone in on a specific test. Add the metadata to the test and only
this test runs, remove it and the whole suite runs:

``` clojure
#kaocha/v1
{:tests [{:focus-meta [:xxx]}]}
```

```clojure
(deftest ^:xxx my-test
  ,,,)
```

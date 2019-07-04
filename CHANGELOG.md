# Unreleased

## Added

## Fixed

## Changed

# 0.0-529 (2019-07-04 / 975bbc6)

## Added

## Fixed

## Changed

# 0.0-521 (2019-06-19 / b652f99)

## Added

- Type hints to eliminate reflection warnings.

## Fixed

- Fix `--watch` when invoked without a `[:kaocha/cli-options :config-file]`,
  either because `tests.edn` doesn't exist, or the config originated elsewhere.
- Make `kaocha.repl/config` set `[:kaocha/cli-options :config-file]` if
  applicable.
- Handle exceptions in `--watch` higher up, to prevent certain errors from being
  silently ignored.

## Changed

- When providing dynamic var bindings in `tests.edn`, we now try to load the
  given namespaces before setting the bindings.

# 0.0-418 (2019-04-11 / d445b44)

## Fixed

- Make sure "no tests found" warning only shows up when it really needs to.

## Changed

- lambdaisland/deep-diff {:mvn/version "0.0-29"} -> {:mvn/version "0.0-47"}
- nubank/matcher-combinators {:mvn/version "0.8.1"} -> {:mvn/version "0.9.0"}

# 0.0-413 (2019-03-30 / 9477eaf)

## Added

- Added a check to make sure org.clojure/tools.cli is up to date.

# 0.0-409 (2019-03-19 / 8f177ea)

## Changed

- Built in plugins in the `kaocha.plugin` can now be specified as simple (rather
  than namespaced) keywords.
- The binding plugin has been removed, instead its functionality is now
  built-in, which allowed us to address several issues.
- Load errors now end in an immediate failure of the test run, instead of a
  warning. They are reported as an error so plugins like the notifier and
  junit.xml can display them.
- dependency upgrades, this fixes an upstream issue with clj-diff
- lambdaisland/deep-diff {:mvn/version "0.0-25"} -> {:mvn/version "0.0-29"}
- orchestra {:mvn/version "2018.12.06-2"} -> {:mvn/version "2019.02.06-1"}

# 0.0-389 (2019-01-29 / 152db39)

## Added

- Added a `pre-report` hook. This allows plugins to inspect and change test
  events just before they are passed to the reporter.
- Added a `:kaocha.plugin/notifier` plugin that pops up desktop notifications
  when a test run passes or fails.
- Add the `wrap-run` hook to the hooks plugin.
- Watch mode: re-run all tests by pressing "enter"
- Watch mode: watch `tests.edn` for changes
- Watch mode: ignore certain files with `:kaocha.watch/ignore [".*" ,,,]`
- To disable output capturing you can now use `:capture-output? false`, instead
  of `:kaocha.plugin.capture-output/capture-output? false`. Since this is a
  built-in plugin that's enabled by default it makes sense to provide a
  shorthand for this.
- Added a `:kaocha.plugin/bindings` plugin that allows setting dynamic var
  bindings from `tests.edn`

## Fixed

- Preserve changes to the config made in a `pre-load` hook
- Watch mode: if a namespace fails to load then report it clearly and fail the
  test run, skipping any remaining tests.

## Changed

- Ignore `--focus-meta` when none of the tests have this particular metadata.
- Print a nicer message when a plugin can't be loaded (Daniel Compton)
- Only print random seed when test run failsg

# 0.0-367 (2019-01-16 / 514765b)

## Added

- Added a "version-filter" plugin, which will skip tests if their metadata
  indicates they are not compatible with the Clojure or Java version being used.

# 0.0-359 (2019-01-15 / 53d06ab)

## Added

- Mark tests with `^:kaocha/pending` metadata to skip over them, and report them
  as "pending"
- Added a "hooks" plugin (`:kaocha.plugin/hooks`), that allows hooking into
  various parts of Kaocha's process using simple functions. This provides a more
  lightweight alternative to end users for full fledged plugins.
- The `pre-test` hook now runs earlier, so that `:kaocha.testable/skip` or
  `:kaocha.testable/pending` can be set from the hook and still be recognized.

# 0.0-343 (2018-12-31 / c38d94f)

## Changed

- [internal] Extracted `kaocha.runner/run`, to be used by alternative command
  line runners like boot.

# 0.0-333 (2018-12-28 / 89b4c13)

## Added

- Added a TAP reporter (`kaocha.report/tap`)
- Added a new `--print-env` flag to the `:kaocha.plugin.alpha/info` plugin,
  which outputs the Clojure and Java version before each run.

## Fixed

- Filter out `jdk.internal.reflect` stack frames when detecting source file (Java 9+)

## Changed

- Prefer a stackframe-based file/line detection over taking the file/line of the
  test definition, this way the reported location is that of the assertion,
  rather than that of the test.
- The `print-invocations` plugin no longer prints out the `--config-file` flag
  when it hasn't changed from its default value (`tests.edn`)

# 0.0-319 (2018-12-12 / 012b4ef)

## Fixed

- Removed debug prn calls

## Added

- [internal] Test types can signal that any remaining sibling tests should be
  skipped. This is used by the ClojureScript test type: if a test times out then
  we can no longer rely on the JavaScript environment being responsive. Instead
  fail the test (signal a timeout) and skip any remaining tests in the same
  suite.

# 0.0-313 (2018-12-10 / b45ccd1)

## Added

- A new work-in-progress information plugin, `:kaocha.plugin.alpha/info`,
  currently only prints the list of all test ids.

## Changed

- When specifying test suites defaults are always provided, so it's no longer
  necessary to provide `:src-paths`, `:test-paths`, etc. if they don't deviate
  from the defaults. This also means all test suites get the default
  `:kaocha.filter/skip-meta [:kaocha/skip]`.

## Fixed

- In watch mode: Scan test-dirs/source-dirs, rather than letting tools.namespace
  derive the list of paths from the classpath

# 0.0-305 (2018-12-07 / 8b51576)

## Fixed

- Honor the `capture-output?` flag when provided in `tests.edn`
- Print captured output when a test fails because it doesn't contain assertions
- Make file/line in failures more accurate

## Added

- Consider `(is (= ))` assertions with only a single argument as failures, as
  these are most likely typos, they always evaluate to true.

## Changed

- Kaocha now also considers the global hierarchy when determining event types.
  This makes it possible for third-party clojure.test reporters to be
  Kaocha-aware without having to depend on Kaocha.
- [lambdaisland/kaocha-cloverage](https://github.com/lambdaisland/kaocha-cloverage)
  is now its own project, make sure to include it in your deps/project files.

# 0.0-266 (2018-11-08 / 0e9d0ee)

## Fixed

- Speeded up startup by avoiding loading kaocha.watch, core.async, fipp, puget [#14](https://github.com/lambdaisland/kaocha/issues/14)

## Added

- Cucumber support, see [lambdaisland/kaocha-cucumber](https://github.com/lambdaisland/kaocha-cucumber)
- Function specs are now checked with orchestra + expound
- Plugins in the `kaocha.plugin` namespace can now be specified on the command line with their short name
- `kaocha.assertions` namespace with custom `clojure.test` assertions. Currently
  for internal use but might evolve into its own library.
- Added Cloverage integration, currently still included in the main `lambdaisland/kaocha` artifact.
- Added support for "pending" tests (skipped but still reported). Currently only used by Cucumber.

## Changed

- This release contains several internal changes to support disparate test suite types.
- The test summary now reads "x tests, y assertions, z failures", rather than "test vars", to be more test type agnostic.

# 0.0-248 (2018-11-01 / d6edc4f)

## Fixed

- De-dupe plugins, for cases where a plugin is added to `tests.edn` and on the CLI

# 0.0-243 (2018-10-31 / 55bb5c1)

## Fixed

- Fix matcher-combinator support

# 0.0-239 (2018-10-31 / f1b9a61)

## Added

- Skip tests marked as `^:kaocha/skip` by default
- Junit.xml output, see [lambdaisland/kaocha-junit-xml](https://github.com/lambdaisland/kaocha-junit-xml)

## Fixed

- Fix Java 11 compatiblity

## Changed

- deep-diff functionality has been extracted into [its own library](https://github.com/lambdaisland/deep-diff)

# 0.0-217 (2018-10-22 / 642cff8)

## Fixed

- Looking up of print-handler fails for nil

# 0.0-211 (2018-10-21 / fd7e623)

## Fixed

- Fix regression in `kaocha.report.progress`

# 0.0-206 (2018-10-21 / 4654f45)

## Added

- `kaocha.plugin.alpha/xfail`, mark failing tests with `^:kaocha/xfail` to make them pass, and vice versa. ([#2](https://github.com/lambdaisland/kaocha/issues/2))
- `(is (= ,,,))` assertions are now deep diffed and pretty printed.
- Plugins now take a "docstring" (added under the `:description` key). (For future use.)

## Fixed

- `--fail-fast` mode is incompatible with the check which fails tests when they don't contain any assertions. ([#10](https://github.com/lambdaisland/kaocha/issues/10))
- `kaocha.repl` does not correctly merge in extra config keys
- Reported line number in case of failures points to the failed assertion, not to the failed var.
- Passing extra config to `kaocha.repl/run` will still by default run the current `*ns*`, rather than all tests.
- Honor `*print-length*` when set. (defaults to 100)
- Make sure `kaocha.testable/*current-testable*` is bound when plugin's `wrap-run` result executes.

## Changed

- Config merging (defaults + tests.edn + repl values) now uses `meta-merge` for flexible append/prepend/replace.
- Print the testable-id in failure messages rather than just the var name, so it can be passed straight on to `--focus`

# 0.0-189 (2018-09-28 / 087b78b)

## Fixed

- Fixed `kaocha.repl/run-all`

# 0.0-185 (2018-09-28 / 15081ed)

## Changed

- BREAKING: `kaocha.repl/run-tests` and `kaocha.repl/run-all-tests` have been
  renamed to `run` and `run-all`, so a `(use 'kaocha.repl)` doesn't clash with
  `clojure.test`.

- Skip reloading namespaces during load if they are already defined. In watch
  mode they still get reloaded through tools.namespace when necessary. This
  change is done to make REPL usage more intuitive. When running
  `kaocha.repl/run-tests` it will refrain from doing a `(require ... :reload)`,
  instead accepting whatever state your REPL process is in.

# 0.0-181 (2018-09-27 / 472f63f)

## Changed

- Documentation updates

# 0.0-176 (2018-09-23 / 7fd6c80)

## Added

- Make Kaocha's reporters more easily extendable through keyword hierarchies

## Fixed

- Load errors in `--watch` mode no longer cause the process to exit. Instead you
  get a warning and the loading is retried on next change.

## Changed

- Made `kaocha.repl` a lot more useful, making it easy to do a full or partial
  test run from a REPL or buffer.

# 0.0-162 (2018-09-20 / dc503f4)

## Added

- Reporters in the `kaocha.report` namespace now can be specified on the command
  line with just their short name, e.g. `--reporter dots`

## Fixed

- Improved matcher-combinators support, now failure summary is only shown at the
  end (for dots or docs reporter), and output is correctly captured and
  displayed.
- Compatibility with newer matcher-combinators.

# 0.0-153 (2018-09-19 / 25a68bd)

## Changed

- BREAKING: Instead of `#kaocha` use `#kaocha/v1` as a reader literal that
  normalizes configuration. The old version is still supported for now but
  generates a warning.

# 0.0-147 (2018-09-19 / 351429d)

## Fixed

- Use tools.namespace.track for tracking/reloading namespaces in watch mode.
  This should make this a lot more reliable.
- Change the `add-classpath` classloader hack so it doesn't mess up the thread
  binding stack.
- Make dots reporter compatible with newer versions of matcher-combinators.

# 0.0-138 (2018-09-17 / 9bc74e4)

## Changed

- Due a limitation of Aero `:ns-patterns` must be strings, and not regex
  literals. Clarified this in the docs.

# 0.0-134 (2018-09-16 / d0da4e2)

## Added

- filter keys (skip/skip-meta/focus/focus-meta) can now be used without namespace when using the `#kaocha` reader literal for configuration. Before: `:kaocha.filter/focus`, after: `:focus`.
- Added a `:kaocha.hooks/pre-load` hook to complement `:kaocha.hooks/post-load`.

# 0.0-122 (2018-09-12 / 735aa75)

## Changed

- BREAKING: `:kaocha.type/suite` is now called `:kaocha.type/clojure.test`

# 0.0-118 (2018-09-09 / cc55d42)

## Added

- `--version` command line flag (only works when running from a JAR)
- `--help` as alternative to `--test-help`, for environments where `--help` isn't shadowed

## Fixed

- Make code base analysable by cljdoc.xyz
- Make sure clojars.org links to correct

# 0.0-97 (2018-09-08 / 734df37)

## Added

- `kaocha.repl/run-tests` / `kaocha.repl/run-all-tests` (since renamed to `run` and `run-all`)

## Fixed

- Dynamically adding test directories to the classpath should be more robust now.

## Changed

- `:kaocha.suite/ns-patterns`, `:kaocha.suite/source-paths` and
- `:kaocha.suite/test-paths` have been renamed to just use the `:kaocha`
namespace.

# b0a70dc267a (2018-07-29)

## Added

- Capture output, this is enabled by default. Only output of failing tests is
  printed. This also introduced a new plugin hook, `wrap-run`, which allows you
  to decorate `run-testables` for doing things like adding bindings.
- Reporters now always get the current testable in their clojure.test reporting
  event.
- Added a progress bar reporter
- Documentation reporter: show in the documentation output which tests fail.

## Fixed

- The randomize plugin could cause an exception because the
  sort-by-random-number-generator wasn't stable (it violated the contract of a
  comparator). Instead assign each testable a sort key first, then sort by
  those. This does mean seeds from before this change will no longer produce the
  same result.
- When specifying an invalid reporter var, error before trying to load tests.
- Correctly count matcher-combinator mismatch failures when exiting early (Ctrl-C).

# Changed

- When running in watch mode, first re-run failed tests. Only when they pass do
  a full re-run.
- When `fail-fast` is true, quit immediately when a load error is detected,
  instead of only failing when the namespace runs.
- Suite names can now be specified on the CLI with keyword syntax, i.e.
  `bin/kaocha :unit`


# 7b79fad92d (2018-06-16)

## Added

- The profiling plugin can now be configured on the command line and from
  tests.edn, with `--[no-]profiling`, `--profiling-count`,
  `:kaocha.plugin.profiling/profiling?`, `:kaocha.plugin.profiling/count`

## Changed

- `--focus` and `--focus-meta` override config-level `:focus`/`:focus-meta`,
  rather than append. This is more intuitive, when focusing from the command
  line you don't want extra tests to show up.
- Don't run the `post-summary` hook when using the API, this prevents noise from
  plugins in the `--print-test-plan` / `--print-result` output.

## Fixed

- Don't count filtered tests in profiling results.

# 8eeff5b340 (2018-06-13)

## Added

- Testable now has an optional `:kaocha.var/wrap` key, which can contain a seq
  of functions that will be used to "wrap" the actual test function, useful e.g.
  for providing bindings. clojure.test style :each fixtures are also handled
  through this key, so plugins can choose to add wrapping functions at the start
  or the end of this seq to wrap "inside" or "around" the fixtures.

# 9a920204bc (2018-06-13)

## Changed

- Make the test-plan available in `pre-test` and `post-test` plugin hooks, so
  that they have access to top level configuration items.

# 3319ed6f81 (2018-06-13)

## Added

- Added the `kaocha.plugin/defplugin` macro, making plugins look more like a
  deftype.

# 9a6fa32592 (2018-06-02)

## Changed

- The configuration format has changed, you should now start with the `#kaocha
  {}` tagged reader literal in `tests.edn` to provide defaults. If you want more
  control then overwrite `tests.edn` with the output of `--print-config` and
  tweak.

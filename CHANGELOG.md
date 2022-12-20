# Unreleased

## Added

## Fixed

- Circular dependencies in watch mode no longer kills the process.

## Changed

# 1.71.1119 (2022-10-24 / 4317878)

## Added

- Configure a timeout for notifications with `--notification-timeout` or
  `:kaocha.plugin.notifier/timeout`. Note that this feature doesn't work for
  terminal-notifier on macOS, with Java's built-in TrayIcon, or with
  notify-send on certain Linux desktop environments and distributions.

## Fixed

- Fix configuration parsing when using `--watch`. Previously, profiles would be
  respected on the initial load, but not after watch reloaded the configuration.
- Notifier now reports errors in your notification command instead of silently
  failing.
- Fixed `java.lang.IllegalArgumentException: No matching clause: []` exception
  when `:kaocha.spec.test.check/syms` is a collection.

# 1.70.1086 (2022-09-19 / f8d8ad5)

## Added

- Add specs for kaocha.plugin/profiling data (#302).

## Fixed

- Fix issue where diffing `(is (= ...))` assertions sometimes fails
  when comparing records.
- Fix the `--no-notifications` flag in the Notifier plugin

## Changed

- Some error codes were duplicated. This is a **breaking change** if you rely on error codes. 
  - When a test suite configuration value is not a collection or symbol, the
    error code is now 250 instead of 252. The error code 252 is still used
    when the configuration file fails to load.
  - When registering a plugin fails due to being unable to load a namespace,
    the error code is now 249 instead of 254. When registering a plugin
    fails for other reasons, the error code is now 248 instead of 254. When
    resolving a reporter var fails, the error code is still 254. You can
    test for error codes between 240 and 249 to check for plugin errors
    regardless of cause.
- Upgraded `lambdaisland/deep-diff` to `lambdaisland/deep-diff2`

# 1.69.1069 (2022-07-26 / 07574ec)

## Fixed

- Fix a regression where in direct usage of `kaocha.api` a reporter is specified as a function

# 1.68.1059 (2022-06-20 / 4425558)

## Fixed

- Fix issue in `gc-profiling`, which was caused by a prior fix from 1.67.1055.

# 1.67.1055 (2022-06-16 / 1682c7e)

## Fixed

- Fix misleading error message when all tests are filtered out. Previously, it
  would misleadingly suggest you correct the `test-paths` and `ns-patterns`
  configuration keys.
- Fix overflow with the `gc-profiling` plugin when there's too many bytes.

# 1.66.1034 (2022-04-26 / 7a5824a)

## Added

- Extend `kaocha.config/load-config` to also work on resources

# 1.65.1029 (2022-04-11 / a60db27)

## Added

## Fixed

- Fix issue with `gc-profiling` plugin when there's a syntax error.
- Ensure that modifications that are done by deleting and recreating the file
  are picked up by using `--watch` with Beholder.

## Changed

# 1.64.1010 (2022-03-17 / e203099)

## Fixed

- Ignore test suites that are already excluded through CLI arguments when
  running the filter plugin. This gets rid of false warnings regarding missing
  metadata

# 1.63.998 (2022-02-15 / ae54f2b)

## Changed

- Version bumps of spec, expound, test.check

## Fixed

- Catch an exception in the notifier plugin which can occur in headless (CI)
  setups

# 1.62.993 (2022-02-04 / 083f69e)

## Added

- Added configuration `:kaocha.watch/type` which takes either `:beholder` or
  `:hawk` as values. Defaulting to `:beholder` as the new fs watcher.
- Add `--no-fail-fast` CLI option

## Fixed

## Changed

- Changed default watcher to [Beholder](https://github.com/nextjournal/beholder)
  which supports OSX/m1 machines natively. Hawk is now deprecated and will be
  removed in a future release.

# 1.60.977 (2022-01-01 / 4a6ed21)

## Added

- Add support for `:watch?` when using `exec-fn`

# 1.60.972 (2021-12-16 / af118c8)

## Fixed

- Fixed an issue where the combination of a load error and using `--focus` would
  result in "no tests found", shadowing the actual error

# 1.60.945 (2021-10-24 / 7ed5dd8)

## Fixed

- Apply `:kaocha/bindings` higher in the stack, so they are visible to `main`
  and `post-summary` plugin hooks
- Fix an issue when the history track reporter gets invoked outside of the scope
  where the history tracking atom is bound
- Fixed issue in `gc-profiling` that caused itermittent `NullPointerException`s

# 1.0.937 (2021-10-20 / 8ccaba7)

## Added

- `kaocha.runner/exec` for use with Clojure CLI's -X feature
- Added `gc-profiling` plugin for measuring the memory usage of tests.

## Fixed

- Breaking! Unqualified plugin names containing dots are no longer
  normalized to contain the `kaocha.plugin`-namespace in front.

# 1.0.902 (2021-10-01 / 3100c8b)

## Added

- Added support for code using `:as-alias`

## Fixed

- Fix only considering public vars when building up the test plan

# 1.0.887 (2021-09-01 / 38470aa)

## Added

## Fixed

- Fix load-error handling in `kaocha.watch`
- Fix `could not resolve symbol require` error that occured sporadically when requiring certain kaocha namespaces.
- Fix printing of boolean options in the print-invocations plugin
- Fix Java reflection warning in the Notifier plugin

## Changed

- [BREAKING] Remove the Orchestra dependency, and no longer auto-instrument.
  You'll have to list Orchestra in your own `deps.edn`/`project.clj` if you want
  to use the Orchestra plugin.
- Version bumps of Clojure, tools.cli, spec.alpha, expound

# 1.0.861 (2021-05-21 / dbfd6e8)

## Added

- Formatting of failed test results using deep-diff can be disabled with `--diff-style :none` on the command line or `:diff-style :none` in `tests.edn`.

## Fixed

- Fix at least some cases of syntax errors being suppressed by the "no tests found" message.

## Changed

# 1.0.829 (2021-03-08 / a88ebda)

## Added

- Kaocha watch can now add ignores from `.gitignore` and `.ignore`. To enable
    this feature, set `:kaocha.watch/use-ignore-file` to true in your deps.edn.
- Kaocha now falls back to the notifications provided by Java's AWT when it can't
    find `notify-send` or `terminal-notifier`.

## Fixed

- Clearly alert the user that Clojure versions before 1.9 aren't supported, rather than
    failing on whatever 1.9 functionality happens to be invoked first.
- Fixed an issue with the definition of spec `:kaocha.test-plan/load-error` that
    caused a ClassCastException whenever a generator was created for it.
- Errors when loading plugins are more specific, specifying which namespaces, if
    any, failed to load.
- Warn when running Kaocha without a configuration file. This is fine for
    experimenting, but for long-term use, we recommend creating a configuration
    file to avoid changes in behavior between releases.
- Provide a warning when no tests are found.
- Fix exception when running Kaocha on Windows with the built-in notification
    plugin enabled.

## Changed

# 1.0.732 (2020-11-26 / b418350)

## Fixed

- Fixed an issue with the optional `clojure.test.check` dependency (follow-up)

# 1.0.726 (2020-11-24 / faa6ef6)

## Fixed

- `If the value of a configuration key is not a collection or symbol,
  a more helpful error message is output. Fixes #124`
- `kaocha.type.spec.test.check` now correctly builds fdef testables with
  configuration options from their enclosing test suites.
- `kaocha.plugin.alpha.spec-test-check` now honors command line arguments based
  upon all of the configured STC suites rather than the static
  `:generative-fdef-checks` selector.
- Fix an issue where `clojure.test.check` would be required for Kaocha to work,
  rather than being an optional dependency

## Changed

- Breaking! Test configuration (`tests.edn`) is now validated with spec, meaning
  existing configs may fail. In most cases you should be able to update your
  config so it is valid, but please do report any issues.
- `kaocha.plugin.alpha.spec-test-check` now respects a priority of supplied
  configuration. CLI options always take precedence, followed by options
  specified in individual test suites, followed by global options.
- Improved spec definitions and generative fdef coverage

# 1.0.700 (2020-09-18 / 552b977)

## Fixed

- Fix documentation table of contents
- Make Ctrl-C (SIGINT) handling more reliable, so you can always short-circuit
  Kaocha to see your failing tests.
- Make spec-test-check plugin honor commond line arguments, so you can run only
  the generated test suite.

## Changed

- Don't run group tests (e.g. namespace) when there are no tests inside it that
  would run (empty or all tests skipped)

# 1.0.690 (2020-09-14 / 8a12b69)

## Fixed

- fdef/spec based tests via plugin: honor `:clojure.spec.test.check/instrument?`
  and `:clojure.spec.test.check/check-asserts?` from `tests.edn`

# 1.0.681 (2020-09-10 / 5031360)

## Added

- Added `:kaocha.plugin/debug` for easy introspection of Kaocha's machinery
- Added docstrings and markdown docs for the Orchestra and Preloads plugins

## Fixed

- In the filter plugin's pre-load early filtering of test suites, check flags
  provided directly in the config, instead of only checking command line
  arguments. This fixes kaocha.repl invocations like
  `(kaocha.repl/run {:kaocha.filter/skip [:unit]})`

# 1.0.672 (2020-08-26 / ff68cf5)

## Changed

- Prevent loading of test suites that are excluded from the run

# 1.0.669 (2020-08-19 / 13abc37)

## Added

- Added internal diagnostics

# 1.0.663 (2020-08-17 / 2a815a3)

## Fixed

- Fix `post-summary` when used from  hooks plugin

# 1.0.658 (2020-08-17 / 22ef88c)

## Added

- Add two new hooks, `:kaocha.hooks/pre-load-test`,
  `:kaocha.hooks/post-load-test`
- Extend the hooks plugin to allow for `:kaocha.hooks/pre-load-test`,
  `:kaocha.hooks/pre-test` / `:kaocha.hooks/post-test` and
  `:kaocha.hooks/post-load-test` hooks to be defined on the testable (i.e. on
  the test suite)
- The `:post-summary` hook can now be used through the hooks plugin (before it
  was only available to plugins)
- Allow test type implementations to add `:kaocha.testable/aliases` to
  testables, these can be used when focusing/skipping

## Changed

- `:kaocha.hooks/before` / `:kaocha.hooks/after` now get converted to
  `:kaocha.hooks/pre-test` / `:kaocha.hooks/post-test` hooks. The former are
  considered deprecated but will continue to work.
- the `post-summary` hook will also be called when invoked via `kaocha.repl`
- `kaocha.testable/test-seq` only returns actual testables, not a top level
  config/test-plan map
- Bumped Orchestra and Expound, this pulled in a breaking change in Orchestra
  where it no longer includes the explained spec error in the exception message.
  To accomodate for this the Orchestra plugin has been updated so the
  explanation appears in the reported output.
- Only instrument lambdaisland/kaocha namespaces with Orchestra. For
  instrumentation of your own code or third party libraries use the `:orchestra`
  plugin.

# 1.0.641 (2020-07-09 / ec75d9c)

## Added

- The hooks plugin now understands two new hooks at the test suite level,
  `:kaocha.hooks/before` and `:kaocha.hooks/after`
- Make the Hawk filesystem watcher configurable with `:kaocha.watch/hawk-opts`

# 1.0.632 (2020-05-13 / 149d913)

## Added

- Added a `:kaocha.testable/skip-add-classpath?` flag on the test suite to
  prevent Kaocha from modifying the classpath

# 1.0.629 (2020-05-02 / 6275298)

## Added

- An Orchestra plugin `:kaocha.plugin/orchestra` for instrumenting
  functions specs with [Orchestra](https://github.com/jeaye/orchestra)
- A Preloads plugin `:kaocha.plugin/preloads` for requiring namespaces
  before Kaocha loads test suites. This is useful for requiring spec
  namespaces or other side-effecting namespaces that are not required
  by test code.

## Fixed

- Fixed an issue where plugin names where not correctly normalized before
  deduplication, leading to potentially having a plugin twice in the stack

## Changed

- Added `:kaocha.report/printed-expression` to the `debug` reporter, for
  debugging reporting issues with kaocha-cljs

# 1.0-612 (2020-03-29 / 06293c8)

## Added

- Kaocha's own plugins can now use a simple keyword in `tests.edn`, e.g.
  `:notifier` instead of `:kaocha.plugin/notifier`, similar to what we do on the
  command line.

## Changed

- Bumped several dependencies: org.clojure/spec.alpha, org.clojure/tools.cli, and aero

# 0.0-601 (2020-03-11 / 6b88d96)

## Fixed

- Namespaces where the ns form can not be read by tools.readers are now reported
  as a test failure, rather than being quietly ignored.

# 0.0-597 (2020-03-10 / 746943b)

## Added

- Added support in the reporter for the `=?` macro as used in Expectations
  (thanks [@dharrigan](https://github.com/dharrigan) 🎉)

# 0.0-590 (2020-02-05 / 70d314f)

## Fixed

- Fix support for dynamic bindings with `set!` in watch mode. (thanks
  [@frenchy64](https://github.com/frenchy64))
- Fixes support for `:config` hooks in the hooks plugin.

# 0.0-581 (2020-01-22 / be2bd38)

## Changed

- Breaking change! Focus/skip options are now applied in two passes, once for
  options in `tests.edn`, once for command-line/REPL options. The result is that
  command line options can only narrow the set of tests to be run. (thanks
  [@otwieracz](https://github.com/otwieracz))

# 0.0-573 (2020-01-13 / 156d084)

## Added

- Added `#meta-merge` reader literal for `tests.edn`. (thanks
  [@RickyMoynihan](https://github.com/RickMoynihan))

# 0.0-565 (2019-12-10 / 72be8ec)

## Fixed

- Fix an issue with the Kaocha keyword hierarchy where two keys had an ancestor
  via two different paths, causing problems when trying to `underive`.
- Make the version-filter plugin work with non-numeric version segments, as in
  `"1.8.0_212-20190523183340.buildslave.jdk8u"`

# 0.0-554 (2019-10-01 / fc5d93a)

## Fixed

- Fix regression, only show the `--focus` warning when applicable

# 0.0-549 (2019-10-01 / aff529c)

## Added

- Added the `--profile` command line flag, which gets passed to Aero's `#profile
  {}` tagged literal reader. Defaults to `:ci` when `CI=true`.
- Output a warning when `--focus TESTABLE-ID` does not match any tests.

# 0.0-541 (2019-09-11 / c97a2cb)

## Fixed

- The `kaocha.report.progress/progress` progress bar reporter now allows the
  appropriate exception to be reported when there is a syntax error in Clojure
  source code. Was formerly throwing NullPointerException.

## Changed

- Consolidate `kaocha.hierarchy`, so it can be used for kaocha-cljs

# 0.0-529 (2019-07-04 / 975bbc6)

## Fixed

- Be smarter about loading namespaces and resolving vars for dynamic bindings
  set in `tests.edn`

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

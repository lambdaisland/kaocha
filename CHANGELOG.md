# Unreleased

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

## Added

## Fixed

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

- `kaocha.repl/run-tests` / `kaocha.repl/run-all-tests`

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

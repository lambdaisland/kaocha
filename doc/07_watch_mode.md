## 7. Watch mode

You can enable watch mode with the `--watch` command line flag. If you want
watch mode to be the default you can also configure it in `tests.edn` with
`:watch? true`. In that case the `--no-watch` flag turns it off again.

When running in watch mode Kaocha will keep an eye on your test and source
directories (as configured on the test suites), as well as your Kaocha
configuration in `tests.edn`. Whenever any of these files changes it tries to
reload the changed files, and then runs your tests again.

Watch mode is based on `tools.namespace`, this library keeps track of the
dependencies between namespaces. When a file changes then any namespace that
depends on it gets unloaded first, completely erasing the namespace and its vars
from Clojure's memory, before loading them again from scratch.

This fixes a lot of issues that are present with more naive code reloading
schemes, but it comes with its own set of caveats. Refer to the [tools.namespace
README](https://github.com/clojure/tools.namespace) for more information.

If any tests fail, then upon the next change first the failed tests will be run.
Only when they pass is the complete suite run again.

Sometimes your source or test directories will contain files that should be
ignored by watch mode, for instance temporary files left by your editor. You can
tell watch mode to ignore these with the `:kaocha.watch/ignore` configuration
key.

This takes a vector of patterns which largely behave like Unix-shell style
"glob" patterns, although they differ from standard shell behavior in some
subtle ways. These are processed using Java's
[PathMatcher](https://docs.oracle.com/javase/10/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
interface, the provided links describes how they work in detail.

``` clojure
#kaocha/v1
{:kaocha.watch/ignore ["*.tmp"]}
```

When running in watch mode you can press the Enter (Return) key to manually
trigger a re-run of the tests. This will always run all tests, not just the
tests that failed on the last run.

Interrupt the process (Ctrl-C) to exit Kaocha's watch mode.

## Tips and tricks

The `:kaocha.plugin/notifier` plugin will cause a system notification to pop up
whenever a test run finishes. This works really well together with watch mode,
as it means you can leave Kaocha running in the background, and only switch over
when a test failed.

Watch mode is most useful when it can provide quick feedback, this is why it's a
good idea to combine it with `--fail-fast`, so you know as soon as possible when
a test fails, and you can focus on one test at a time. You can disable
randomization (`--no-randomize`) to prevent having to jump back and forth
between different failing tests.

If your test suites takes a long time to run then watch mode will be a lot less
effective, in that case consider tagging your slowest tests with metadata, and
filtering them out. (`--skip-meta :slow`)

`tests.edn` is also watched for changes, and gets reloaded on every run, so
there's a lot you can do without having to restart Kaocha.

- enable `:fail-fast? true`
- focus on specific tests or namespaces
- enable extra plugins
- set a fixed seed (when debugging ordering issue)
- switch to a different reporter

You can tell Kaocha to ignore changes to files matching patterns in `.gitignore`
or `.ignore` files by setting `:kaocha.watch/use-ignore-file` to `true` in your deps.edn.

Currently these features of `.gitignore` are not supported:
- Negating patterns. Git allows you to specify patterns for files that should *not* be
    ignored.
- Directory-only patterns. PatternMatcher doesn't distinguish between files and
    directories in paths.

## Configuring the watcher

Kaocha uses [Beholder](https://github.com/nextjournal/beholder) to watch the
filesystem for changes. By default Beholder will pick a mechanism suitable for
your operating system. Beholder works with OSX and Apple Silicon mac machines as
well.

Previously Kaocha used another watcher, [Hawk](https://github.com/wkf/hawk), which
has been deprecated in favour of Beholder. If you still want to use it for some
reason, then use the `:kaocha.watch/type :hawk` configuration to switch to Hawk.
Please note that Hawk will be removed completely in a future release.

Use the `:kaocha.watch/hawk-opts` configuration key to pass options to Hawk.
Currently it understands `:watcher` (`:java`, `:barbary`, `:polling`) and
`:sensitivity` (`:high`, `:medium`, `:low`, only applies to `:polling`).

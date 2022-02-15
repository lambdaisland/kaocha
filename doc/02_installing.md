## 2. Installing

Kaocha is distributed through [Clojars](https://clojars.org), with the
identifier `lambdaisland/kaocha`. You can find version information for the
latest release at [https://clojars.org/lambdaisland/kaocha](https://clojars.org/lambdaisland/kaocha).

The main namespace for use at the command line is `kaocha.runner`, regardless of which tool you're using to invoke Clojure.

For example:

``` shell
clojure -Sdeps '{:deps {lambdaisland/kaocha {:mvn/version "1.63.998"}}}' -m kaocha.runner --test-help
```

Below are instructions on the recommended way to set things up for various build tools.

### Clojure CLI / deps.edn

In `deps.edn`, create a `test` "alias" (profile) that loads the `lambdaisland/kaocha` dependency.

``` clojure
;; deps.edn
{:deps { ,,, }
 :aliases
 {:test {:extra-deps {lambdaisland/kaocha {:mvn/version "1.63.998"}}}}}
```

Other dependencies that are only used for tests, like test framework or assertion
libraries, can also go here.

Next create a `bin/kaocha` wrapper script. Having it in this location is
strongly recommended, as it's where developers coming from other projects will
expect to find it.

In it invoke `clojure` with the `:test` alias and the `kaocha.runner` main
namespace. This is what `bin/kaocha` by default looks like. Make sure to add
`"$@"` so that any arguments to `bin/kaocha` are passed on to `kaocha.runner`.

``` shell
#!/usr/bin/env bash

clojure -A:test -m kaocha.runner "$@"
```

Make sure the script is executable:

``` shell
chmod +x bin/kaocha
```

This script provides a useful place to encode extra flags or setup that is
needed in order for tests to run correctly.

``` shell
#!/usr/bin/env bash

. secrets.env
clojure -J-Xmx512m -A:dev:test -m kaocha.runner --config-file test/tests.edn "$@"
```

This version also sets an alternative location for Kaocha's configuration file:
`tests.edn`. It is generally recommended to leave it at the root of the project,
but if you do want to move or rename it this is the way to go.

`--config-file` is the only Kaocha option that makes sense in this script, other
Kaocha configuration should be done through `tests.edn`.

Now you can invoke Kaocha as such:

``` shell
bin/kaocha --version
```

#### Alternative method: :exec-fn

We also support using the Clojure CLI `:exec-fn`/`-X`. However, we recommend the
binstub approach above because it allows you to use traditional long and short
options.  If you nonetheless prefer `:exec-fn`/`-X`, you can set up `deps.edn`:

```clojure
;; deps.edn
{:deps { ,,, }
 :aliases 
 {:test {:extra-deps {lambdaisland/kaocha {:mvn/version "1.63.998"}}
         :exec-fn kaocha.runner/exec-fn
         :exec-args {}}}}
```

And then Kaocha can be invoked this way: `clojure -X:test`

Generally speaking, we recommend using `tests.edn` for all of your configuration
rather than putting it in `exec-args` unless there's an alternative combination
of options you frequently run.

In that case, you can put configuration options `:exec-args` as though it were
`tests.edn`. Let's say you frequently use watch with `:fail-fast` and a subset
of tests skipped. You could save that configuration with an additional alias:
`clojure -X:watch-test` like so:


```clojure
;; deps.edn
{:deps { ,,, }
 :aliases 
 {:test {:extra-deps {lambdaisland/kaocha {:mvn/version "1.63.998"}}
         :exec-fn kaocha.runner/exec-fn
         :exec-args {}}
 :watch-test {:extra-deps {lambdaisland/kaocha {:mvn/version "1.63.998"}}
         :exec-fn kaocha.runner/exec-fn
         :exec-args {:watch? true
	 :skip-meta :slow
	 :fail-fast? true }}}}
```

If you wanted to turn off `fail-fast` temporarily, you could run `clojure
-X:watch-test :fail-fast? false`.

### Leiningen

Add Kaocha to your `:dev` profile, then add an alias that invokes `lein run -m kaocha.runner`:

``` clojure
(defproject my-proj "0.1.0"
  :dependencies [,,,]
  :profiles {:dev {:dependencies [,,, [lambdaisland/kaocha "1.63.998"]]}}
  :aliases {"kaocha" ["run" "-m" "kaocha.runner"]})
```

Now you can invoke Kaocha as such:

``` shell
lein kaocha --version
```

It is still recommeded to create a `bin/kaocha` wrapper for consistency among
projects. The rest of the documentation assumes you can invoke Kaocha with
`bin/kaocha`.

``` shell
#!/usr/bin/env bash

lein kaocha "$@"
```

For more information on `:dev`, see the [Leiningen docs](https://cljdoc.org/d/leiningen/leiningen/2.9.3/doc/profiles#default-profiles).

#### Alternative method: separate `:kaocha` profile

If you want to use Kaocha only in certain circumstances, say when
[profiling tests](08_plugins.md#profiling), you may not want to add it to your `:dev` profile.

Instead, add a `:kaocha` profile with the Kaocha dependency, then add an
alias that activates the profile and invokes `lein run -m kaocha.runner`:

``` clojure
(defproject my-proj "0.1.0"
  :dependencies [,,,]
  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "1.63.998"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})
```

Invoking Kaocha and creating `bin/kaocha` will work the same way. However,
Kaocha will not be available in your REPL by default.

### Boot

See [kaocha-boot](https://github.com/lambdaisland/kaocha-boot) for instructions.

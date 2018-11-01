## 2. Installing

Kaocha is distributed through [Clojars](https://clojars.org), with the
identifier `lambdaisland/kaocha`. You can find version information for the
latest release at [https://clojars.org/lambdaisland/kaocha](https://clojars.org/lambdaisland/kaocha).

The main namespace for use at the command line is `kaocha.runner`, regardless of which tool you're using to invoke Clojure.

For example:

``` shell
clojure -Sdeps '{:deps {lambdaisland/kaocha {:mvn/version "0.0-248"}}}' -m kaocha.runner --test-help
```

Below are instructions on the recommended way to set things up for various build tools.

### Clojure CLI / deps.edn

In `deps.edn`, create a `test` "alias" (profile) that loads the `lambdaisland/kaocha` dependency.

``` clojure
;; deps.edn
{:deps { ,,, }
 :aliases
 {:test {:extra-deps {lambdaisland/kaocha {:mvn/version "0.0-248"}}}}}
```

Other dependencies that are only used for tests like test framework or assertion
libraries can also go here.

Next create a `bin/kaocha` wrapper script. Having it in this location is
strongly recommended, as its where developers coming from other projects will
expect to find it.

In it invoke `clojure` with the `:test` alias and the `kaocha.runner` main
namespace. This is what `bin/kaocha` by default looks like. Make sure to add
`"$@"` so that any arguments to `bin/kaocha` are passed on to `kaocha.runner`.

``` shell
#!/bin/bash

clojure -A:test -m kaocha.runner "$@"
```

Make sure the script is executable

``` shell
chmod +x bin/kaocha
```

This script provides a useful place to encode extra flags or setup that is
needed in order for tests to run correctly.

``` shell
#!/bin/bash

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

### Leiningen

Add a `:kaocha` profile, where the Kaocha dependency is included, then add an
alias that activates the profile, and invokes `lein run -m kaocha.runner`.

``` clojure
(defproject my-proj "0.1.0"
  :dependencies [,,,]
  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "0.0-248"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})
```

Now you can invoke Kaocha as such:

``` shell
lein kaocha --version
```

It is still recommeded to create a `bin/kaocha` wrapper for consistency among
projects. The rest of the documentation assumes you can invoke Kaocha with
`bin/kaocha`.

```
#!/bin/bash

lein kaocha "$@"
```

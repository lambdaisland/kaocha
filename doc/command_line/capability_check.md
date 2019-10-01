# Capability check for org.clojure/tools.cli If a project's dependency

pulls in an old version of tools.cli, then this may break command line flags
  of the form `--[no-]xxx`. Before starting the main command line runner, Kaocha
  verifies that tools.cli has the necessary capabilities.

## With an outdated tools.cli

- <em>When </em> I run `clojure -Sdeps '{:deps {org.clojure/tools.cli {:mvn/version "0.3.5"}}}' --main kaocha.runner`

- <em>Then </em> stderr should contain:

``` nil
org.clojure/tools.cli does not have all the capabilities that Kaocha needs. Make sure you are using version 0.3.6 or greater.
```




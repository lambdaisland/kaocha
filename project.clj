(defproject lambdaisland/kaocha "0.1.0"
  :dependencies [[org.clojure/clojure "1.10.0-alpha7"]
                 [clojure.java-time "0.3.2"]
                 [org.clojure/spec.alpha "0.1.143"]
                 [org.clojure/tools.cli "0.3.7"]
                 [io.aviso/pretty "0.1.34"]
                 [aero "1.1.3"]
                 [org.clojure/tools.namespace "0.3.0-alpha4"]
                 [slingshot "0.12.2"]
                 [hawk "0.2.11"]
                 [progrock "0.1.2"]
                 [org.tcrawley/dynapath "1.0.0"]
                 [expound "0.6.0"]
                 [org.clojure/core.async "0.4.474"]
                 [nubank/matcher-combinators "0.2.5"]]

  :aliases {"kaocha" ["run" "-m" "kaocha.runner"]})

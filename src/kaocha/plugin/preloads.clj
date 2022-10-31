(ns kaocha.plugin.preloads
  "Preload namespaces

  Useful for preloading specs and other instrumentation.

  This plugin calls `require` on the given namespace names before loading any
  tests.

  This plugin works for only Clojure namespaces. For ClojureScript namespaces,
  use the :preloads functionality of the ClojureScript compiler."
  (:require [kaocha.plugin :refer [defplugin]]))

(defplugin kaocha.plugin/preloads
  (pre-load [config]
    (when-let [ns-names (::ns-names config)]
      (apply require ns-names))
    config))

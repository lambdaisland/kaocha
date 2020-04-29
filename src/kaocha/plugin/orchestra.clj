(ns kaocha.plugin.orchestra
  (:require [kaocha.plugin :refer [defplugin]]
            [orchestra.spec.test :as orchestra]))

(defplugin kaocha.plugin/orchestra
  (post-load [test-plan]
     ;; Instrument specs after all of the test namespaces have been loaded
     (orchestra/instrument)
     test-plan)

  (post-run [result]
     ;; Unstrument specs after tests have run. This isn't so important
     ;; for CLI testing as the process will exit shortly after the post-run
     ;; step, but is helpful for running Kaocha tests from the REPL.
     (orchestra/unstrument)
     result))

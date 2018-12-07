(ns kaocha.plugin.alpha.info
  (:require [kaocha.api :as api]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [slingshot.slingshot :refer [throw+]]))

(def cli-opts
  [[nil "--print-test-ids" "Print all known test ids"]])

(defplugin kaocha.plugin.alpha/info
  (cli-options [opts]
    (into opts cli-opts))

  (main [config]
    (if (:print-test-ids (:kaocha/cli-options config))
      (let [test-plan (api/test-plan config)]
        (doseq [test (testable/test-seq test-plan)]
          (println (:kaocha.testable/id test)))
        (throw+ {:kaocha/early-exit 0}))
      config)))

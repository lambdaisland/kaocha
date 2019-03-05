(ns kaocha.plugin.alpha.info
  (:require [kaocha.api :as api]
            [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [slingshot.slingshot :refer [throw+]]))

(def cli-opts
  [[nil "--print-test-ids" "Print all known test ids"]
   [nil "--print-env" "Print Clojure and Java version."]])

(defplugin kaocha.plugin.alpha/info
  (cli-options [opts]
    (into opts cli-opts))

  (main [config]
    (cond
      (:print-test-ids (:kaocha/cli-options config))
      (binding [api/*active?* true]
        (let [test-plan (api/test-plan (plugin/run-hook :kaocha.hooks/config config))]
          (doseq [test (testable/test-seq test-plan)]
            (println (:kaocha.testable/id test)))
          (throw+ {:kaocha/early-exit 0})))

      (:print-env (:kaocha/cli-options config))
      (do
        (println "Clojure" (clojure-version))
        (println (System/getProperty "java.runtime.name") (System/getProperty "java.runtime.version"))
        config)

      :else
      config)))

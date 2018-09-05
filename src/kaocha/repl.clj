(ns kaocha.repl
  (:require [kaocha.config :as config]
            [kaocha.plugin :as plugin]
            [kaocha.api :as api]
            [kaocha.result :as result]))

(defn config [& args]
  (let [[config-file opts] (if (string? (first args))
                             [(first args) (apply hash-map (next args))]
                             ["tests.edn" (apply hash-map args)])
        config             (-> (config/load-config config-file)
                               (config/apply-cli-opts opts))
        plugin-chain       (plugin/load-all (:kaocha/plugins config))]
    (plugin/with-plugins plugin-chain
      (plugin/run-hook :kaocha.hooks/config config))))

(defn test-plan [& args]
  (let [config       (apply config args)
        plugin-chain (plugin/load-all (:kaocha/plugins config))]
    (plugin/with-plugins plugin-chain
      (api/test-plan config))))

(defn run-tests
  ([]
   (run-tests *ns*))
  ([ns]
   (let [config (config "--focus"
                        (name (.name (the-ns *ns*))))]
     (api/run config))))

(defn run-all-tests [& args]
  (result/totals (:kaocha.result/tests (api/run (apply config args)))))

#_
(run-all-tests)

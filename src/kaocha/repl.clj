(ns kaocha.repl
  (:require [kaocha.config :as config]
            [kaocha.plugin :as plugin]
            [kaocha.api :as api]))

(defn config [& args]
  (let [[config-file opts] (if (string? (first args))
                             [(first args) (apply hash-map (next args))]
                             ["tests.edn" (apply hash-map args)])
        config             (-> (config/load-config config-file)
                               (config/apply-cli-opts opts))
        plugin-chain       (plugin/load-all (:kaocha/plugins config))]
    (binding [plugin/*current-chain* plugin-chain]
      (plugin/run-hook plugin-chain :kaocha.hooks/config config))))

(defn test-plan [& args]
  (let [config       (apply config args)
        plugin-chain (plugin/load-all (:kaocha/plugins config))]
    (binding [plugin/*current-chain* plugin-chain]
      (api/test-plan config))))

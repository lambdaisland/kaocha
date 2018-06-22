(ns kaocha.config2
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [kaocha.output :as out]))

(defn load-config
  ([]
   (load-config "tests.edn"))
  ([path]
   (let [file (io/file path)]
     (if (.exists file)
       (aero/read-config file)
       (out/warn "Config file not found: " path ", using default values.")))))

(defn apply-cli-opts [config options]
  (cond-> config
    (:fail-fast options) (assoc :kaocha/fail-fast? true)
    true                 (assoc :kaocha/cli-options options)))

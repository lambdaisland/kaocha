(ns lambdaisland.kaocha.config
  (:require [clojure.java.io :as io]
            [lambdaisland.kaocha.output :as out]))

(def default-config-file "tests.edn")

(defn- default-config []
  (read-string (slurp (io/resource "lambdaisland/kaocha/default_config.edn"))))

(defn- load-config* [path]
  (let [file (io/file path)]
    (if (.exists file)
      (read-string (slurp file))
      (out/warn "Config file not found: " path ", using default values."))))

(defn- rename-key [m old-key new-key]
  (if (contains? m old-key)
    (assoc (dissoc m old-key) new-key (get m old-key))
    m))

(defn load-config [file]
  (->> (load-config* file)
       (merge (default-config))))

(defn merge-options [config options]
  (merge config
         (-> options
             (rename-key :test-path :test-paths)
             (rename-key :ns-pattern :ns-patterns))))

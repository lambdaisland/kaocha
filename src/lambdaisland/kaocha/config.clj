(ns lambdaisland.kaocha.config
  "Read,validate, normalize configuration as found in tests.edn or passed in
  through command line options."
  (:require [clojure.java.io :as io]
            [lambdaisland.kaocha.output :as out]))

(def global-opts #{:reporter :color :suites :only-suites})
(def suite-opts #{:id :test-paths :ns-patterns})

(defn default-config []
  (read-string (slurp (io/resource "lambdaisland/kaocha/default_config.edn"))))

(defn load-config [path]
  (let [file (io/file path)]
    (if (.exists file)
      (read-string (slurp file))
      (out/warn "Config file not found: " path ", using default values."))))

(defn- rename-key [m old-key new-key]
  (if (contains? m old-key)
    (assoc (dissoc m old-key) new-key (get m old-key))
    m))

(defn normalize-cli-opts [opts]
  (-> opts
      (rename-key :test-path :test-paths)
      (rename-key :ns-pattern :ns-patterns)))

(defn filter-suites [suite-ids suites]
  (if (seq suite-ids)
    (filter #(some #{(name (:id %))} suite-ids) suites)
    suites))

(defn resolve-reporter [reporter]
  (cond
    (symbol? reporter)
    (do
      (require (symbol (namespace reporter)))
      @(resolve reporter))

    (seqable? reporter)
    (let [rs (map resolve-reporter reporter)]
      (fn [m] (run! #(% m) rs)))

    :else
    reporter))

(defn normalize [config]
  (let [global    (select-keys config global-opts)
        suite     (select-keys config suite-opts)]
    (-> global
        (update :suites (fn [suites]
                          (->> suites
                               (mapv (partial merge suite))))))))

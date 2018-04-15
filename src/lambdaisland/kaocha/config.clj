(ns lambdaisland.kaocha.config
  "Read,validate, normalize configuration as found in tests.edn or passed in
  through command line options."
  (:require [clojure.java.io :as io]
            [lambdaisland.kaocha.output :as out]
            [lambdaisland.kaocha.report :as report]
            [slingshot.slingshot :refer [throw+]]
            [lambdaisland.kaocha :as k]))

(def global-opts #{:reporter :color :suites :only-suites :fail-fast})
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
    (filter #(some #{(:id %)} suite-ids) suites)
    suites))

(defn resolve-reporter [reporter]
  (cond
    (= 'clojure.test/report reporter)
    report/clojure-test-report

    (symbol? reporter)
    (do
      (try
        (require (symbol (namespace reporter)))
        (catch java.io.FileNotFoundException e
          (throw+ {::k/reporter-not-found reporter})))
      (if-let [resolved (resolve reporter)]
        (resolve-reporter @resolved)
        (throw+ {::k/reporter-not-found reporter})))

    (seqable? reporter)
    (let [rs (map resolve-reporter reporter)]
      (fn [m] (run! #(% m) rs)))

    :else
    reporter))

(defn normalize [config]
  (let [config (merge (default-config) config)
        global (select-keys config global-opts)
        suite  (select-keys config suite-opts)]
    (-> global
        (update :suites (fn [suites]
                          (->> suites
                               (mapv (partial merge suite))))))))

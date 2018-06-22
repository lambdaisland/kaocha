(ns kaocha.config2
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [kaocha.output :as out]
            [kaocha.report :as report]
            [slingshot.slingshot :refer [throw+]]))

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

(defn resolve-reporter [reporter]
  (cond
    (= 'clojure.test/report reporter)
    report/clojure-test-report

    (symbol? reporter)
    (do
      (try
        (if-let [ns (namespace reporter)]
          (require (symbol ns))
          (throw+ {:kaocha/reporter-not-found reporter}))
        (catch java.io.FileNotFoundException e
          (throw+ {:kaocha/reporter-not-found reporter})))
      (if-let [resolved (resolve reporter)]
        (resolve-reporter @resolved)
        (throw+ {:kaocha/reporter-not-found reporter})))

    (seqable? reporter)
    (let [rs (doall (map resolve-reporter reporter))]
      (fn [m] (run! #(% m) rs)))

    :else
    reporter))

(ns kaocha.config2
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [kaocha.output :as out]
            [kaocha.report :as report]
            [slingshot.slingshot :refer [throw+]]))

(defn- rename-key [m old-key new-key]
  (if (contains? m old-key)
    (assoc (dissoc m old-key) new-key (get m old-key))
    m))

(defn default-config []
  (aero/read-config (io/resource "kaocha/default_config.edn")))


(defmethod aero/reader 'kaocha [opts tag value]
  (let [default-config (default-config)
        {:keys [tests
                plugins
                reporter
                color?
                fail-fast?
                watch?]} value
        tests (seq
               (map (fn [m]
                      (-> m
                          (rename-key :type :kaocha.testable/type)
                          (rename-key :id :kaocha.testable/id)
                          (rename-key :test-paths :kaocha.suite/test-paths)
                          (rename-key :source-paths :kaocha.suite/source-paths)
                          (rename-key :ns-patterns :kaocha.suite/ns-patterns)
                          (cond->> (not (:type m)) (merge (first (:kaocha/tests default-config))))))
                    tests))]
    (cond-> default-config
      tests      (assoc :kaocha/tests tests)
      plugins    (update :kaocha/plugins into plugins)
      reporter   (assoc :kaocha/reporter reporter)
      color?     (assoc :kaocha/color? color?)
      fail-fast? (assoc :kaocha/fail-fast? fail-fast?)
      watch?     (assoc :kaocha/watch? watch?))))

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
    (:reporter options)  (assoc :kaocha/reporter (:reporter options))
    (:watch options)     (assoc :kaocha/watch? (:watch options))
    true                 (assoc :kaocha/cli-options options)))

(defn apply-cli-args [config args]
  (if (seq args)
    (update config
            :kaocha/tests
            (fn [tests]
              (map #(if (contains? (set args) (name (:kaocha.testable/id %)))
                      %
                      (assoc % :kaocha.testable/skip true))
                   tests)))
    config))
#_
(defn add-built-ints [config]
  (-> config
      (update :kaocha/plugins #(into [:kaocha.plugin.randomize] %))
      (update :kaocha/reporter #((if (seqable? %) into conj)
                                 '[kaocha.report/report-counters
                                   kaocha.history/track
                                   kaocha.report/dispatch-extra-keys]
                                 %))))

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

(ns kaocha.config
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

(defn normalize [value]
  (let [default-config   (default-config)
        {:keys [tests
                plugins
                reporter
                color?
                fail-fast?
                watch?]} value
        tests            (some->> tests
                                  (mapv (fn [m]
                                          (-> m
                                              (rename-key :type :kaocha.testable/type)
                                              (rename-key :id :kaocha.testable/id)
                                              (rename-key :test-paths :kaocha/test-paths)
                                              (rename-key :source-paths :kaocha/source-paths)
                                              (rename-key :ns-patterns :kaocha/ns-patterns)
                                              (rename-key :skip :kaocha.filter/skip)
                                              (rename-key :focus :kaocha.filter/focus)
                                              (rename-key :skip-meta :kaocha.filter/skip-meta)
                                              (rename-key :focus-meta :kaocha.filter/focus-meta)
                                              (cond->> (not (:type m)) (merge (first (:kaocha/tests default-config))))))))]
    (cond-> default-config
      tests              (assoc :kaocha/tests tests)
      plugins            (update :kaocha/plugins into plugins)
      reporter           (assoc :kaocha/reporter reporter)
      (some? color?)     (assoc :kaocha/color? color?)
      (some? fail-fast?) (assoc :kaocha/fail-fast? fail-fast?)
      (some? watch?)     (assoc :kaocha/watch? watch?)
      :->                (merge (dissoc value :tests :plugins :reporter :color? :fail-fast? :watch?)))))

(defmethod aero/reader 'kaocha [opts tag value]
  (out/warn "The #kaocha reader literal is deprecated, please change it to #kaocha/v1.")
  (normalize value))

(defmethod aero/reader 'kaocha/v1 [opts tag value]
  (normalize value))

(defn load-config
  ([]
   (load-config "tests.edn"))
  ([path]
   (let [file (io/file path)]
     (if (.exists file)
       (aero/read-config file)
       (normalize {})))))

(defn apply-cli-opts [config options]
  (cond-> config
    (some? (:fail-fast options)) (assoc :kaocha/fail-fast? true)
    (:reporter options)          (assoc :kaocha/reporter (:reporter options))
    (:watch options)             (assoc :kaocha/watch? (:watch options))
    (some? (:color options))     (assoc :kaocha/color? (:color options))
    (:plugin options)            (update :kaocha/plugins #(distinct (concat % (:plugin options))))
    true                         (assoc :kaocha/cli-options options)))

(defn apply-cli-args [config args]
  (if (seq args)
    (update config
            :kaocha/tests
            (fn [tests]
              (mapv #(if (contains? (set args) (:kaocha.testable/id %))
                       %
                       (assoc % :kaocha.testable/skip true))
                    tests)))
    config))

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

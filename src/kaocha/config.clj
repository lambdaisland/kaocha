(ns kaocha.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [kaocha.output :as output]
            [kaocha.report :as report]
            [slingshot.slingshot :refer [throw+]]
            [meta-merge.core :refer [meta-merge]]))

(defn- rename-key [m old-key new-key]
  (if (contains? m old-key)
    (assoc (dissoc m old-key) new-key (get m old-key))
    m))

(defn default-config []
  (aero/read-config (io/resource "kaocha/default_config.edn")))

(defn normalize-test-suite [m]
  (let [m (-> m
              (rename-key :type :kaocha.testable/type)
              (rename-key :id :kaocha.testable/id)
              (rename-key :test-paths :kaocha/test-paths)
              (rename-key :source-paths :kaocha/source-paths)
              (rename-key :ns-patterns :kaocha/ns-patterns)
              (rename-key :skip :kaocha.filter/skip)
              (rename-key :focus :kaocha.filter/focus)
              (rename-key :skip-meta :kaocha.filter/skip-meta)
              (rename-key :focus-meta :kaocha.filter/focus-meta)
              (cond->> (not (:type m))
                (merge (first (:kaocha/tests (default-config))))))]
    (assoc m :kaocha.testable/desc (str (name (:kaocha.testable/id m))
                                        " (" (name (:kaocha.testable/type m)) ")"))))

(defn normalize [config]
  (let [default-config   (default-config)
        {:keys [tests
                plugins
                reporter
                color?
                fail-fast?
                randomize?
                watch?]} config
        tests            (some->> tests (mapv normalize-test-suite))]
    (cond-> {}
      tests              (assoc :kaocha/tests (vary-meta tests assoc :replace true))
      plugins            (assoc :kaocha/plugins plugins)
      reporter           (assoc :kaocha/reporter (vary-meta reporter assoc :replace true))
      (some? color?)     (assoc :kaocha/color? color?)
      (some? fail-fast?) (assoc :kaocha/fail-fast? fail-fast?)
      (some? watch?)     (assoc :kaocha/watch? watch?)
      (some? randomize?) (assoc :kaocha.plugin.randomize/randomize? randomize?)
      :->                (merge (dissoc config :tests :plugins :reporter :color? :fail-fast? :watch?)))))

(defn replace-by-default [config k]
  (if-let [v (get config k)]
    (if (#{:prepend :append} (meta v))
      config
      (update config k vary-meta assoc :replace true))
    config))

(defn merge-config [c1 c2]
  (meta-merge c1 (-> c2
                     (replace-by-default :kaocha/reporter)
                     (replace-by-default :kaocha/tests))))

(defmethod aero/reader 'kaocha [opts tag value]
  (output/warn "The #kaocha reader literal is deprecated, please change it to #kaocha/v1.")
  (-> (default-config)
      (merge-config (normalize value))))

(defmethod aero/reader 'kaocha/v1 [opts tag value]
  (-> (default-config)
      (merge-config (normalize value))))

(defn load-config
  ([]
   (load-config "tests.edn"))
  ([path]
   (let [file (io/file path)]
     (if (.exists file)
       (aero/read-config file)
       (default-config)))))

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

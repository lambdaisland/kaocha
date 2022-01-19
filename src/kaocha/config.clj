(ns kaocha.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [kaocha.output :as output]
            [kaocha.report :as report]
            [slingshot.slingshot :refer [throw+]]
            [meta-merge.core :refer [meta-merge]]))

;the reader literal for the current default:
(def current-reader 'kaocha/v1)

(defn default-config []
  (aero/read-config (io/resource "kaocha/default_config.edn")))

(defn- rename-key [m old-key new-key]
  (if (contains? m old-key)
    (assoc (dissoc m old-key) new-key (get m old-key))
    m))

(defn replace-by-default [config k]
  (if-let [v (get config k)]
    (if (#{:prepend :append} (meta v))
      config
      (if (or (coll? v)
              (symbol? v))
        (update config k vary-meta assoc :replace true)
        (do
         (output/error "Test suite configuration value with key " k " should be a collection or symbol, but got '" v "' of type " (type v))
         (throw+ {:kaocha/early-exit 252}))))
    config))

(defn merge-config [c1 c2]
  (meta-merge c1 (-> c2
                     (replace-by-default :kaocha/reporter)
                     (replace-by-default :kaocha/tests)
                     (replace-by-default :kaocha/test-paths)
                     (replace-by-default :kaocha/source-paths)
                     (replace-by-default :kaocha/ns-patterns))))

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
              (rename-key :focus-meta :kaocha.filter/focus-meta))]
    (as-> m $
      (merge-config (first (:kaocha/tests (default-config))) $)
      (merge {:kaocha.testable/desc (str (name (:kaocha.testable/id $))
                                         " (" (name (:kaocha.testable/type $)) ")")}
             $))))

(defn normalize-plugin-names [plugins]
  (mapv (fn [p]
          (cond
            (qualified-keyword? p) p
            (simple-keyword? p) (keyword "kaocha.plugin" (name p))
            :else
            (throw (ex-info "Plugin name must be a keyword" {:plugins plugins}))))
        plugins))

(defn normalize [config]
  (let [default-config (default-config)
        {:keys [:tests
                :plugins
                :reporter
                :color?
                :fail-fast?
                :diff-style
                :randomize?
                :capture-output?
                :watch?
                :bindings]} config
        tests (some->> tests (mapv normalize-test-suite))]
    (cond-> {}
      tests                   (assoc :kaocha/tests (vary-meta tests assoc :replace true))
      plugins                 (assoc :kaocha/plugins plugins)
      reporter                (assoc :kaocha/reporter (vary-meta reporter assoc :replace true))
      bindings                (assoc :kaocha/bindings bindings)
      (some? color?)          (assoc :kaocha/color? color?)
      (some? fail-fast?)      (assoc :kaocha/fail-fast? fail-fast?)
      (some? diff-style)      (assoc :kaocha/diff-style diff-style)
      (some? watch?)          (assoc :kaocha/watch? watch?)
      (some? randomize?)      (assoc :kaocha.plugin.randomize/randomize? randomize?)
      (some? capture-output?) (assoc :kaocha.plugin.capture-output/capture-output? capture-output?)
      :->                     (merge (dissoc config :tests :plugins :reporter :color? :fail-fast? :watch? :randomize?)))))

(defmethod aero/reader 'kaocha [_opts _tag value]
  (output/warn (format "The #kaocha reader literal is deprecated, please change it to %s." current-reader))
  (-> (default-config)
      (merge-config (normalize value))))

(defmethod aero/reader 'kaocha/v1 [_opts _tag value]
  (-> (default-config)
      (merge-config (normalize value))))

(defmethod aero/reader 'meta-merge [_opts _tag value]
  (apply meta-merge value))

(defn load-config
  ([]
   (load-config "tests.edn"))
  ([path]
   (load-config path {}))
  ([path opts]
   (let [file (io/file path)
         profile (:profile opts (if (= (System/getenv "CI") "true")
                                  :ci
                                  :default))]
     (if (.exists file)
       (aero/read-config file {:profile profile})
       (default-config)))))

(defn apply-cli-opts [config options]
  (cond-> config
    (some? (:fail-fast options))  (assoc :kaocha/fail-fast? true)
    (:reporter options)           (assoc :kaocha/reporter (:reporter options))
    (:watch options)              (assoc :kaocha/watch? (:watch options))
    (some? (:color options))      (assoc :kaocha/color? (:color options))
    (some? (:diff-style options)) (assoc :kaocha/diff-style (:diff-style options))
    (:plugin options)             (update :kaocha/plugins #(distinct (concat % (:plugin options))))
    true                          (assoc :kaocha/cli-options options)))

(defn apply-cli-args [config args]
  (if (seq args)
    (-> config
        (assoc :kaocha/cli-args args)
        (update :kaocha/tests
                (fn [tests]
                  (let [run-suite? (set args)]
                    (mapv (fn [{suite-id :kaocha.testable/id :as suite}]
                            (cond-> suite
                              (not (run-suite? suite-id))
                              (assoc :kaocha.testable/skip true)))
                          tests)))))
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

(defn binding-map
  "Get the dynamic bindings configured in the configuration, and turn them into a
  var->value mapping to be used with [[clojure.core/with-bindings]].

  This will ignore unkown vars/namespaces, because they may not have loaded yet."
  ([config]
   (binding-map config false))
  ([config throw-errors?]
   (into {}
         (keep
          (fn [[k v]]
            (try
              ;; already loaded
              [(find-var k) v]
              (catch java.lang.IllegalArgumentException e
                (try
                  ;; not loaded yet, try to load
                  (when-let [ns (namespace k)]
                    (require (symbol ns)))
                  [(find-var k) v]
                  ;; Still no good? Possibly ignore because we add more
                  ;; directories to the classpath during the load phase, and
                  ;; then try again.
                  (catch java.io.FileNotFoundException e ;; require failed
                    (when throw-errors? (throw e)))
                  (catch java.lang.IllegalArgumentException e ;; find-var failed
                    (when throw-errors? (throw e))))))))
         (:kaocha/bindings config))))

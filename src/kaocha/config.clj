(ns kaocha.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [kaocha.output :as output]
            [kaocha.report :as report]
            [kaocha.plugin :as plugin]
            [kaocha.specs :as specs]
            [slingshot.slingshot :refer [throw+]]
            [meta-merge.core :refer [meta-merge]])
  (:import (java.io File)
           (java.net URL)))

;; the reader literal for the current default:
(def current-reader 'kaocha/v1)

(defn default-config []
  (aero/read-config (io/resource "kaocha/default_config.edn")))

(defn- rename-key [m old-key new-key]
  (if (contains? m old-key)
    (assoc (dissoc m old-key) new-key (get m old-key))
    m))

(defn replace-by-default [config k]
  (if-let [v (get config k)]
    (if (some #{:prepend :append} (keys (meta v)))
      config
      (if (or (coll? v)
              (symbol? v)
              (fn? v))
        (update config k vary-meta assoc :replace true)
        (do
          (output/error "Test suite configuration value with key " k " should be a collection or symbol, but got '" v "' of type " (type v))
          (throw+ {:kaocha/early-exit 250}))))
    config))

(defn merge-config
  "Applies meta-merge to c1 and c2, using ^:replace for certain keys if no
  metadata is specified."
  [c1 c2]
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
        {:keys [tests
                plugins
                reporter
                color?
                fail-fast?
                diff-style
                randomize?
                capture-output?
                watch?
                bindings]} config
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

(defn read-config-source [source {:aero/keys [read-config-opts] :as opts}]
  (let [profile (:profile opts (if (= (System/getenv "CI") "true")
                                 :ci
                                 :default))]
    (aero/read-config source (merge {:profile profile}
                                    read-config-opts))))

(defprotocol ConfigSource
  (read-config [source opts]))

(extend-protocol ConfigSource

  nil ;; handle nil source case
  (read-config [_ _]
    (default-config))

  Object ;; keep existing default behaviour
  (read-config [path opts]
    (read-config (io/file path) opts))

  File
  (read-config [^File file opts]
    (when (.exists file)
      ;; Note: since the default :resolver in Aero is aero/adaptive-resolver,
      ;; #include will first check if there is a resource with that name
      ;; and will only revert to a file if not, unless overridden in
      ;; :aero/read-config-opts of opts
      (read-config-source file opts))))

(extend-protocol ConfigSource
  URL ;; resource
  (read-config [resource opts]
    (when resource
      ;; Note: we only #include resources
      ;; unless overridden in :aero/read-config-opts of opts
      (->> (update opts
                   :aero/read-config-opts
                   #(merge {:resolver aero/resource-resolver} %))
           (read-config-source resource)))))


(defn load-config-file
  "Loads and returns configuration from `source` or the file \"tests.edn\"
  if called without arguments.

  If the config value loaded from `source` is nil, it returns the default
  configuration, which is the result of `(default-config)`.

  Accepts various types as `source`:
  - `File`   loads from a file from the file system, provided it exists
  - `Object` coerces to `File`, treated as the single argument to (io/file),
              like a string path
  - `URL`    treated as a resource on the classpath
  - `nil`    returns the default configuration

  The list of supported types can be extended by extending
  the `ConfigSource` protocol.

  `opts` can be used to affect some aspects of loading, which is dependent on
  the `source`'s `ConfigSource` implementation. For the `source`s supported
  out of the box, [aero](https://github.com/juxt/aero) is used to parse the raw
  data. It uses `opts` in the following way:

  `(:profile opts)` can be specified to select an Aero profile
  `(:aero/read-config-opts opts)` is passed to `aero/read-config`

  By default, when loading from something that coerces to a `File`, Aero will
  try to resolve `#include` references as resources first, then files. This is
  the default behaviour of Aero (`aero/adaptive-resolver`) and is kept for
  backwards-compatibility.

  When loading from a resource (`URL`), we tell Aero to only try resolving
  `#include`s to resources (`aero/resource-resolver`) to try and be less
  surprising.

  These default choices can be overridden by setting another resolver in `opts`:

  `{:aero/read-config-opts {:resolver resolver-to-use}}`"
  ([]
   (load-config-file (io/file "tests.edn")))
  ([source]
   (load-config-file source {}))
  ([source opts]
   (if-some [config (read-config source opts)]
     config
     (read-config nil opts))))

;Alias for backward compatibility
(def load-config load-config-file)

(defn apply-cli-opts [config options]
  (cond-> config
    (some? (:fail-fast options))  (assoc :kaocha/fail-fast? (:fail-fast options))
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

(defn load-config2
  "Loads config from config-file, factoring in profile specified using profile,
  and displaying messages about any errors."
  ([config-file]
   (load-config2 config-file nil {} nil nil))
  ([config-file profile]
   (load-config2 config-file profile {} nil nil))
  ([config-file profile opts]
   (load-config2 config-file profile opts nil nil))
  ([config-file profile opts cli-options cli-args]
   (let [config (cond-> config-file
                    true (load-config-file (if profile (assoc opts :profile profile) opts))
                    cli-options (apply-cli-opts cli-options)
                    cli-args (apply-cli-args cli-args))

         check_config_file (when (not (. (io/file (or config-file "tests.edn")) exists))
                             (output/warn (format (str "Did not load a configuration file and using the defaults.\n"
                                          "This is fine for experimenting, but for long-term use, we recommend creating a configuration file to avoid changes in behavior between releases.\n"
                                          "To create a configuration file using the current defaults and configuration file location, create a file named %s that contains '#%s {}'.")
                                                  config-file
                                     current-reader)))
         check  (try
                  (specs/assert-spec :kaocha/config config)
                  (catch AssertionError e
                    (output/error "Invalid configuration file:\n"
                                  (.getMessage e))
                    (throw+ {:kaocha/early-exit 252}))) ]
     (cond-> config
       check_config_file (update ::warnings conj check_config_file)
       check (update ::warnings conj check)))))

;;Do we really need this?
(defn plugin-chain-from-config [config cli-options]
  (plugin/load-all (concat (:kaocha/plugins config) (when cli-options (:plugin cli-options)))))

(defn reload-config [config plugin-chain]
  (if-let [config-file (get-in config [:kaocha/cli-options :config-file])]
    (let [profile (get-in config [:kaocha/cli-options :profile])]
      [(load-config2 config-file profile) plugin-chain])
    [config plugin-chain]))

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

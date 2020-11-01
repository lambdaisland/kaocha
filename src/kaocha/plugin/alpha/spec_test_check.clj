(ns kaocha.plugin.alpha.spec-test-check
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.specs]
            [kaocha.type.spec.test.check]
            [meta-merge.core :refer [meta-merge]]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(s/def ::config-path
  (s/coll-of keyword? :min-count 1))

(s/def ::config-mapping
  (s/map-of ::config-path ::config-path))

(s/def ::stc-config
  (s/keys :opt [::stc/instrument?
                ::stc/check-asserts?
                ::stc/opts]))

(s/def ::cli ::stc-config)
(s/def ::global ::stc-config)

(s/def ::stc-configs
  (s/keys :opt [::cli ::global]))

(s/def ::test
  (s/or :stc-test   :kaocha.type/spec.test.check
        :other-test :kaocha/testable))

(s/def ::tests
  (s/coll-of ::test))

(s/fdef is-stc?
  :args (s/cat :test ::test)
  :ret  boolean?)

(defn ^:private is-stc?
  [test]
  (= (:kaocha.testable/type test) :kaocha.type/spec.test.check))

(def ^:private default-stc-suite
  {:kaocha.testable/type        :kaocha.type/spec.test.check
   :kaocha.testable/id          :generative-fdef-checks
   :kaocha.filter/skip-meta     [:kaocha/skip :no-gen]
   :kaocha/source-paths         ["src"]
   :kaocha.spec.test.check/syms :all-fdefs})

(def ^:private config-paths
  [[::stc/instrument?]
   [::stc/check-asserts?]
   [::stc/opts :num-tests]
   [::stc/opts :max-size]])

(def ^:private cli-config-paths
  (mapv (partial vector :kaocha/cli-options)
        [:stc-instrumentation
         :stc-asserts
         :stc-num-tests
         :stc-max-size]))

(s/fdef make-config-mapping
  :args (s/cat :instrument?-path    ::config-path
               :check-asserts?-path ::config-path
               :opts-num-tests-path ::config-path
               :opts-max-size-path  ::config-path)
  :ret  (s/map-of ::config-path ::config-path
                  :count (count config-paths)))

(defn ^:private make-config-mapping
  [instrument?-path check-asserts?-path opts-num-tests-path opts-max-size-path]
  (let [paths [instrument?-path
               check-asserts?-path
               opts-num-tests-path
               opts-max-size-path]]
    (apply hash-map (interleave paths config-paths))))

(def ^:private config-mappings
  {::cli    (apply make-config-mapping cli-config-paths)
   ::global (apply make-config-mapping config-paths)})

(s/fdef override-test-stc-config
  :args (s/cat :tests       (s/nilable ::tests)
               :stc-configs ::stc-configs)
  :ret  ::tests)

(defn ^:private override-test-stc-config
  [tests stc-configs]
  (map (fn [test]
         (if (is-stc? test)
           (meta-merge (::global stc-configs) test (::cli stc-configs))
           test))
       tests))

(s/fdef extract-stc-config
  :args (s/cat :config         :kaocha/config
               :config-mapping ::config-mapping)
  :ret  :kaocha/config)

(defn ^:private extract-stc-config
  [config config-mapping]
  (reduce-kv (fn [acc source-path dest-path]
               (if-some [setting (get-in config source-path)]
                 (assoc-in acc dest-path setting)
                 acc))
             {}
             config-mapping))

(s/fdef extract-stc-configs
  :args (s/cat :config :kaocha/config)
  :ret  ::stc-configs)

(defn ^:private extract-stc-configs
  [config]
  (reduce-kv (fn [acc key config-mapping]
               (assoc acc key (extract-stc-config config config-mapping)))
             {}
             config-mappings))

(s/fdef override-stc-config
  :args (s/cat :config :kaocha/config)
  :ret  :kaocha/config)

(defn ^:private override-stc-config
  [config]
  (let [stc-configs (extract-stc-configs config)]
    (-> config
        (meta-merge (::cli stc-configs))
        (update :kaocha/tests override-test-stc-config stc-configs))))

(s/fdef ensure-stc-suite
  :args (s/cat :tests (s/nilable ::tests))
  :ret  ::tests)

(defn ^:private ensure-stc-suite
  [tests]
  (if (some is-stc? tests)
    tests
    (conj tests default-stc-suite)))

(def ^:private extract-stc-suite-ids-xform
  (comp (filter is-stc?)
        (map :kaocha.testable/id)))

(s/fdef stc-suite-ids
  :args (s/cat :config :kaocha/config)
  :ret  (s/coll-of keyword? :kind set?))

(defn ^:private stc-suite-ids
  [config]
  (into #{} extract-stc-suite-ids-xform (:kaocha/tests config)))

(s/fdef stc-suites-not-selected?
  :args (s/cat :config :kaocha/config))

(defn ^:private stc-suites-not-selected?
  [{:kaocha/keys [cli-args] :as config}]
  (and (seq cli-args)
       (not-any? (stc-suite-ids config) cli-args)))

(s/fdef config-hook
  :args (s/cat :config :kaocha/config)
  :ret  :kaocha/config)

(defn ^:private config-hook
  [config]
  (let [config-with-suite (update config :kaocha/tests ensure-stc-suite)]
    (if (stc-suites-not-selected? config-with-suite)
      config
      (override-stc-config config-with-suite))))

(defplugin kaocha.plugin.alpha/spec-test-check
  (cli-options [opts]
    (conj opts
          [nil "--[no-]stc-instrumentation" "spec.test.check: Turn on orchestra instrumentation during fdef checks"]
          [nil "--[no-]stc-asserts"         "spec.test.check: Run s/check-asserts during fdef checks"]
          [nil "--stc-num-tests NUM"        "spec.test.check: Test iterations per fdef"
           :parse-fn #(Integer/parseInt %)]
          [nil "--stc-max-size SIZE"        "spec.test.check: Maximum length of generated collections"
           :parse-fn #(Integer/parseInt %)]))
  (config [config]
    (config-hook config)))

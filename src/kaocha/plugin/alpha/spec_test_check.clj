(ns kaocha.plugin.alpha.spec-test-check
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.specs]
            [kaocha.type.spec.test.check]
            [orchestra.core :refer [defn-spec]]
            [meta-merge.core :refer [meta-merge]]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(spec/def ::config-path
  (spec/coll-of keyword? :min-count 1))

(def ^:private config-paths
  [[::stc/instrument?]
   [::stc/check-asserts?]
   [::stc/opts :num-tests]
   [::stc/opts :max-size]])

(spec/def ::config-mapping
  (spec/map-of ::config-path ::config-path
            :count        (count config-paths)))

(spec/def ::stc-config
  (spec/keys :opt [::stc/instrument?
                ::stc/check-asserts?
                ::stc/opts]))

(spec/def ::cli ::stc-config)
(spec/def ::global ::stc-config)

(spec/def ::stc-configs
  (spec/keys :opt [::cli ::global]))

(spec/def ::test
  (spec/or :stc-test   :kaocha.type/spec.test.check
        :other-test :kaocha/testable))

(spec/def ::tests
  (spec/coll-of ::test))

(defn-spec ^:private is-stc? boolean?
  [test ::test]
  (= (:kaocha.testable/type test) :kaocha.type/spec.test.check))

(def ^:private default-stc-suite
  {:kaocha.testable/type        :kaocha.type/spec.test.check
   :kaocha.testable/id          :generative-fdef-checks
   :kaocha.filter/skip-meta     [:kaocha/skip :no-gen]
   :kaocha/source-paths         ["src"]
   :kaocha.spec.test.check/syms :all-fdefs})

(def ^:private cli-config-paths
  (mapv (partial vector :kaocha/cli-options)
        [:stc-instrumentation
         :stc-asserts
         :stc-num-tests
         :stc-max-size]))

(defn-spec ^:private make-config-mapping ::config-mapping
  [instrument?-path    ::config-path
   check-asserts?-path ::config-path
   opts-num-tests-path ::config-path
   opts-max-size-path  ::config-path]
  (let [paths [instrument?-path
               check-asserts?-path
               opts-num-tests-path
               opts-max-size-path]]
    (apply hash-map (interleave paths config-paths))))

(def ^:private config-mappings
  {::cli    (apply make-config-mapping cli-config-paths)
   ::global (apply make-config-mapping config-paths)})

(defn-spec ^:private override-test-stc-config ::tests
  [tests       (spec/nilable ::tests)
   stc-configs ::stc-configs]
  (map (fn [test]
         (if (is-stc? test)
           (meta-merge (::global stc-configs) test (::cli stc-configs))
           test))
       tests))

(defn-spec ^:private extract-stc-config :kaocha/config
  [config         :kaocha/config
   config-mapping ::config-mapping]
  (reduce-kv (fn [acc source-path dest-path]
               (if-some [setting (get-in config source-path)]
                 (assoc-in acc dest-path setting)
                 acc))
             {}
             config-mapping))

(defn-spec ^:private extract-stc-configs ::stc-configs
  [config :kaocha/config]
  (reduce-kv (fn [acc key config-mapping]
               (assoc acc key (extract-stc-config config config-mapping)))
             {}
             config-mappings))

(defn-spec ^:private override-stc-config :kaocha/config
  [config :kaocha/config]
  (let [stc-configs (extract-stc-configs config)]
    (-> config
        (meta-merge (::cli stc-configs))
        (update :kaocha/tests override-test-stc-config stc-configs))))

(defn-spec ^:private ensure-stc-suite ::tests
  [tests (spec/nilable ::tests)]
  (if (some is-stc? tests)
    tests
    (conj tests default-stc-suite)))

(def ^:private extract-stc-suite-ids-xform
  (comp (filter is-stc?)
        (map :kaocha.testable/id)))

(defn-spec ^:private stc-suite-ids (spec/coll-of keyword?)
  [config :kaocha/config]
  (into #{} extract-stc-suite-ids-xform (:kaocha/tests config)))

(defn-spec ^:private stc-suites-not-selected? any?
  [{:kaocha/keys [cli-args] :as config} :kaocha/config]
  (and (seq cli-args)
       (not-any? (stc-suite-ids config) cli-args)))

(defn-spec ^:private config-hook :kaocha/config
  [config :kaocha/config]
  (let [config-with-suite (update config :kaocha/tests ensure-stc-suite)]
    (if (stc-suites-not-selected? config-with-suite)
      config
      (override-stc-config config-with-suite))))

(defplugin kaocha.plugin.alpha/spec-test-check
  (cli-options [opts]
    (conj opts
          [nil "--[no-]stc-instrumentation" "spec.test.check: Turn on orchestra instrumentation during fdef checks"]
          [nil "--[no-]stc-asserts"         "spec.test.check: Run spec/check-asserts during fdef checks"]
          [nil "--stc-num-tests NUM"        "spec.test.check: Test iterations per fdef"
           :parse-fn #(Integer/parseInt %)]
          [nil "--stc-max-size SIZE"        "spec.test.check: Maximum length of generated collections"
           :parse-fn #(Integer/parseInt %)]))
  (config [config]
    (config-hook config)))

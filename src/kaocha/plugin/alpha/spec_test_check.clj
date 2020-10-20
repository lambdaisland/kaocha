(ns kaocha.plugin.alpha.spec-test-check
  (:require [clojure.spec.alpha]
            [clojure.spec.test.alpha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.specs]
            [meta-merge.core :refer [meta-merge]]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

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

(def ^:private cli-paths
  (mapv (partial vector :kaocha/cli-options)
        [:stc-instrumentation
         :stc-asserts
         :stc-num-tests
         :stc-max-size]))

(defn ^:private make-config-mapping
  [instrument?-path check-asserts?-path opts-num-tests-path opts-max-size-path]
  (let [paths [instrument?-path
               check-asserts?-path
               opts-num-tests-path
               opts-max-size-path]]
    (apply hash-map (interleave paths config-paths))))

(def ^:private config-mappings
  {:cli    (apply make-config-mapping cli-paths)
   :global (apply make-config-mapping config-paths)})

(defn ^:private override-test-stc-config
  [tests stc-configs]
  (map (fn [test]
         (if (is-stc? test)
           (meta-merge (:global stc-configs) test (:cli stc-configs))
           test))
       tests))

(defn ^:private update-tests
  [config & args]
  (apply update config :kaocha/tests args))

(defn ^:private extract-stc-config
  [config config-mapping]
  (reduce-kv (fn [acc source-path dest-path]
               (if-some [setting (get-in config source-path)]
                 (assoc-in acc dest-path setting)
                 acc))
             {}
             config-mapping))

(defn ^:private extract-stc-configs
  [config]
  (reduce-kv (fn [acc key config-mapping]
               (assoc acc key (extract-stc-config config config-mapping)))
             {}
             config-mappings))

(defn ^:private override-stc-config
  [config]
  (let [stc-configs (extract-stc-configs config)]
    (-> config
        (meta-merge (:cli stc-configs))
        (update-tests override-test-stc-config stc-configs))))

(defn ^:private ensure-stc-suite
  [tests]
  (if (some is-stc? tests)
    tests
    (conj tests default-stc-suite)))

(def ^:private extract-testable-ids-xform
  (comp (filter is-stc?) (map :kaocha.testable/id)))

(defn ^:private stc-suite-ids
  [config]
  (into #{} extract-testable-ids-xform (:kaocha/tests config)))

(defn ^:private stc-suites-not-selected?
  [{:kaocha/keys [cli-args] :as config}]
  (and (seq cli-args)
       (not-any? (stc-suite-ids config) cli-args)))

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
    (let [config-with-suite (update-tests config ensure-stc-suite)]
      (if (stc-suites-not-selected? config-with-suite)
        config
        (override-stc-config config-with-suite)))))

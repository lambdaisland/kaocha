(ns kaocha.plugin.alpha.spec-test-check
  (:require [clojure.spec.alpha]
            [clojure.spec.test.alpha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.specs]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(def ^:private is-stc? (comp #{:kaocha.type/spec.test.check}
                             :kaocha.testable/type))

(defn ^:private has-stc? [tests]
  (some is-stc? tests))

(defn ^:private tests-with-overridden-stc-opts
  [{:kaocha/keys [tests] ::stc/keys [opts] :as config}]
  (map (fn [test]
         (if (is-stc? test)
           (update test assoc ::stc/opts opts)
           test))
       tests))

(defn ^:private default-test-suite [{::stc/keys [opts] :as config}]
  {:kaocha.testable/type        :kaocha.type/spec.test.check
   :kaocha.testable/id          :generative-fdef-checks
   :kaocha.filter/skip-meta     [:kaocha/skip :no-gen]
   :kaocha/source-paths         ["src"],
   :kaocha.spec.test.check/syms :all-fdefs
   ::stc/opts                   opts})

(defn ^:private override-stc-settings [config]
  (assoc config :kaocha/tests (tests-with-overridden-stc-opts config)))

(defn ^:private add-default-test-suite [config]
  (update config :kaocha/tests conj (default-test-suite config)))

(defplugin kaocha.plugin.alpha/spec-test-check
  (cli-options [opts]
    (conj opts
          [nil "--[no-]stc-instrumentation" "spec.test.check: Turn on orchestra instrumentation during fdef checks"]
          [nil "--[no-]stc-asserts"         "spec.test.check: Run s/check-asserts during fdef checks"]
          [nil "--stc-num-tests NUM"        "spec.test.check: Test iterations per fdef"
           :parse-fn #(Integer/parseInt %)]
          [nil "--stc-max-size SIZE"        "spec.test.check: Maximum length of generated collections"
           :parse-fn #(Integer/parseInt %)]))
  (config [{:kaocha/keys [tests cli-args] :as config}]
    (if (and (seq cli-args)
             (not (some #{:generative-fdef-checks} cli-args)))
      config
      (let [num-tests       (get-in config [:kaocha/cli-options :stc-num-tests])
            max-size        (get-in config [:kaocha/cli-options :stc-max-size])
            instrumentation (get-in config
                                    [:kaocha/cli-options :stc-instrumentation]
                                    (::stc/instrument? config))
            spec-asserts    (get-in config
                                    [:kaocha/cli-options :stc-asserts]
                                    (::stc/check-asserts? config))
            config          (if (has-stc? tests)
                              (override-stc-settings config)
                              (add-default-test-suite config))]
        (assoc config
               ::stc/opts (->> {:num-tests num-tests
                                :max-size  max-size}
                               (filter second)
                               (into {}))
               ::stc/instrument? instrumentation
               ::stc/check-asserts? spec-asserts)))))

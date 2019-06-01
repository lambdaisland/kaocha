(ns kaocha.plugin.alpha.spec-test-check
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [kaocha.hierarchy :as kaocha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.specs]
            [kaocha.testable :as default-test-suite]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.test.check :as type.stc]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(def is-stc? (comp #{:kaocha.type/clojure.spec.test.check}
                   :kaocha.testable/type))

(defn has-stc? [tests]
  (some is-stc? tests))

(defn tests-with-overridden-stc-opts
  [{:kaocha/keys [tests] ::stc/keys [opts] :as config}]
  (map (fn [test]
         (if (is-stc? test)
           (update test assoc ::stc/opts opts)
           test))
       tests))

(defn default-test-suite [{::stc/keys [opts] :as config}]
  {:kaocha.testable/type    :kaocha.type/clojure.spec.test.check
   :kaocha.testable/id      :generative-fdef-checks
   :kaocha.filter/skip-meta [:kaocha/skip :no-gen]
   :kaocha/source-paths     ["src"],
   ::type.stc/syms          :all-fdefs
   ::stc/opts               opts})

(defn overide-stc-settings [config]
  (assoc config :kaocha/tests (tests-with-overridden-stc-opts config)))

(defn add-default-test-suite [config]
  (update config :kaocha/tests conj (default-test-suite config)))

(defplugin kaocha.plugin.alpha/spec-test-check
  (pre-load [{:kaocha/keys [tests] :as config}]
    (if (has-stc? tests)
      (overide-stc-settings config)
      (add-default-test-suite config)))
  (cli-options [opts]
    (conj opts
          [nil  "--[no-]instrumentation" "Turn on orchestra instrumentation during fdef checks"]
          [nil  "--[no-]spec-asserts" "Run s/check-asserts during fdef checks"]
          [nil  "--num-tests NUM" "Test iterations per fdef"
           :parse-fn #(Integer/parseInt %)]
          [nil  "--max-size SIZE" "Maximum length of generated collections"
           :parse-fn #(Integer/parseInt %)]))
  (config [config]
          (let [num-tests       (get-in config [:kaocha/cli-options :num-tests])
                max-size        (get-in config [:kaocha/cli-options :max-size])
                instrumentation (get-in config [:kaocha/cli-options :instrumentation])
                spec-asserts    (get-in config [:kaocha/cli-options :spec-asserts])]
            (assoc config
                   ::stc/opts {:num-tests num-tests
                               :max-size  max-size}
                   ::stc/instrument? instrumentation
                   ::stc/check-asserts? spec-asserts))))

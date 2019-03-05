(ns kaocha.runner
  "Main entry point for command line use."
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [kaocha.config :as config]
            [kaocha.output :as output]
            [kaocha.api :as api]
            [kaocha.jit :refer [jit]]
            [slingshot.slingshot :refer [try+ throw+]]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as clojure.spec]
            [expound.alpha :as expound]
            [orchestra.spec.test :as orchestra]))

(orchestra/instrument)

(defn- accumulate [m k v]
  (update m k (fnil conj []) v))

(defn parse-kw [value]
  (keyword (if (= \: (first value)) (subs value 1) value)))

(def ^:private cli-options
  [["-c" "--config-file FILE"   "Config file to read."
    :default "tests.edn"]
   [nil  "--print-config"       "Print out the fully merged and normalized config, then exit."]
   [nil  "--print-test-plan"    "Load tests, build up a test plan, then print out the test plan and exit."]
   [nil  "--print-result"       "Print the test result map as returned by the Kaocha API."]
   [nil  "--fail-fast"          "Stop testing after the first failure."]
   [nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."]
   [nil  "--[no-]watch"         "Watch filesystem for changes and re-run tests."]
   [nil  "--reporter SYMBOL"    "Change the test reporter, can be specified multiple times."
    :parse-fn (fn [s]
                (let [sym (symbol s)]
                  (if (qualified-symbol? sym)
                    sym
                    (symbol "kaocha.report" s))))
    :assoc-fn accumulate]
   [nil "--plugin KEYWORD"      "Load the given plugin."
    :parse-fn (fn [s]
                (let [kw (parse-kw s)]
                  (if (qualified-keyword? kw)
                    kw
                    (keyword "kaocha.plugin" s))))
    :assoc-fn accumulate]
   [nil "--version"             "Print version information and quit."]

   ;; Clojure CLI tools intercepts --help, so we add --test-help, but in other
   ;; circumstances it should still work.
   [nil "--help"                "Display this help message."]
   ["-H" "--test-help"          "Display this help message."]])

(defn load-props [file-name]
  (with-open [^java.io.Reader reader (io/reader file-name)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) v])))))

(defn kaocha-version []
  (let [resource (io/resource "META-INF/maven/lambdaisland/kaocha/pom.properties")]
    (str "lambdaisland/kaocha " (pr-str (some-> resource load-props :version)))))

(defn run [{:keys [config errors options suites summary]}]
  (let [all-suites     (into #{} (map :kaocha.testable/id) (:kaocha/tests config))
        unknown-suites (set/difference (set suites) all-suites)]
    (cond
      (seq errors)
      (do
        (run! println errors)
        (println summary)
        -1)

      (or (:help options) (:test-help options))
      (do (println summary) 0)

      (:version options)
      (do (println (kaocha-version)) 0)

      (:print-config options)
      (binding [clojure.core/*print-namespace-maps* false]
        (pprint/pprint (plugin/run-hook :kaocha.hooks/config config))
        0)

      (:print-test-plan options)
      (binding [clojure.core/*print-namespace-maps* false]
        (pprint/pprint (api/test-plan (plugin/run-hook :kaocha.hooks/config config)))
        0)

      (seq unknown-suites)
      (do
        (println (str "No such suite: "
                      (str/join ", " (sort unknown-suites))
                      ", valid options: "
                      (str/join ", " (sort all-suites))
                      "."))
        -2)

      (:kaocha/watch? config)
      (do
        ((jit kaocha.watch/run) config)
        @(promise))

      (:print-result options)
      (let [result (api/run (assoc config :kaocha/reporter []))
            totals (result/totals (:kaocha.result/tests result))]
        (binding [clojure.core/*print-namespace-maps* false]
          (pprint/pprint result))
        (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255))

      :else
      (do
        (plugin/run-hook :kaocha.hooks/main config)
        (let [result (plugin/run-hook :kaocha.hooks/post-summary (api/run config))
              totals (result/totals (:kaocha.result/tests result))]
          (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255))))))

(defn- working-tools-cli?
  "Check if the version of clojure.tools.cli in use is able to handle on/off
  boolean flags."
  []
  (= {:foo true}
     (:options
      (cli/parse-opts ["--foo"] [[nil "--[no-]foo"]]))))

(defn- -main* [& args]
  (when-not (working-tools-cli?)
    (output/error "org.clojure/tools.cli does not have all the capabilities that Kaocha needs. Make sure you are using version 0.3.6 or greater.")
    (throw+ {:kaocha/early-exit 253}))

  (binding [clojure.spec/*explain-out* expound/printer]
    (let [{{:keys [config-file plugin]} :options}    (cli/parse-opts args cli-options)
          config                                     (-> config-file
                                                         (or "tests.edn")
                                                         config/load-config)
          plugin-chain                               (plugin/load-all (concat (:kaocha/plugins config) plugin))
          cli-options                                (plugin/run-hook* plugin-chain :kaocha.hooks/cli-options cli-options)
          {:keys [errors options arguments summary]} (cli/parse-opts args cli-options)
          config                                     (-> config
                                                         (config/apply-cli-opts options)
                                                         (config/apply-cli-args (map parse-kw arguments)))
          suites                                     (into #{} (map parse-kw) arguments)]
      (plugin/with-plugins plugin-chain
        (run {:config config
              :options options
              :errors errors
              :suites suites
              :summary (str "USAGE:\n\n"
                            "bin/kaocha [OPTIONS]... [TEST-SUITE]...\n\n"
                            summary
                            "\n\nOptions may be repeated multiple times for a logical OR effect.")})))))

(defn -main [& args]
  (try+
   (System/exit (apply -main* args))
   (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
     (System/exit exit-code))))

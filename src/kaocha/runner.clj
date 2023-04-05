(clojure.core/require 'kaocha.version-check)
(ns kaocha.runner
  "Main entry point for command line use."
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [expound.alpha :as expound]
            [kaocha.api :as api]
            [kaocha.config :as config]
            [kaocha.jit :refer [jit]]
            [kaocha.output :as output]
            [kaocha.plugin :as plugin]
            [kaocha.result :as result]
            [kaocha.specs :as specs]
            [slingshot.slingshot :refer [throw+ try+]])
  (:import (java.io File)))

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
   [nil  "--[no-]fail-fast"     "Stop testing after the first failure."]
   [nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."]
   [nil  "--[no-]watch"         "Watch filesystem for changes and re-run tests."]
   [nil  "--reporter SYMBOL"    "Change the test reporter, can be specified multiple times."
    :parse-fn (fn [s]
                (let [sym (symbol s)]
                  (if (qualified-symbol? sym)
                    sym
                    (symbol "kaocha.report" s))))
    :assoc-fn accumulate]
   [nil "--diff-style STYLE"    "The style of diff to print on failing tests, either :none or :deep"
    :parse-fn parse-kw]
   [nil "--plugin KEYWORD"      "Load the given plugin."
    :parse-fn (fn [s]
                (let [kw (parse-kw s)]
                  (if (qualified-keyword? kw)
                    kw
                    (keyword "kaocha.plugin" s))))
    :assoc-fn accumulate]
   [nil "--profile KEYWORD"     "Configuration profile. Defaults to :default or :ci."
    :parse-fn parse-kw]
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
  ;; TODO: we're calling the config hook here in multiple places, and it's also
  ;; being called in `kaocha.api`. Given that we already need the fully expanded
  ;; config here (since now we're potentially adding suites in the config hook),
  ;; we should call it once at the top here, and pass the processed config into
  ;; kaocha.api. Punting on that because it requires a coordinated update in
  ;; kaocha.repl and kaocha-boot.
  (let [all-suites     (into #{}
                             (map :kaocha.testable/id)
                             (->> config
                                  (plugin/run-hook :kaocha.hooks/config)
                                  :kaocha/tests))
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
      (let [[exit-code finish!] ((jit kaocha.watch/run) config)]
        @exit-code)

      (:print-result options)
      (let [result (api/run (assoc config :kaocha/reporter []))
            totals (result/totals (:kaocha.result/tests result))]
        (binding [clojure.core/*print-namespace-maps* false]
          (pprint/pprint result))
        (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255))

      :else
      (with-bindings (config/binding-map config)
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

  (binding [spec/*explain-out* expound/printer]
    (let [{{:keys [config-file plugin arguments profile]} :options} (cli/parse-opts args cli-options)
          config-file                                               (config/find-config-and-warn config-file)
          ;; Initial configuration load to determine plugins.
          config                                                    (->  (config/load-config config-file (if profile {:profile profile} {}))
                                                                         (config/apply-cli {} (map parse-kw arguments))
                                                                         (config/validate!))
          plugin-chain                                              (plugin/load-all (concat (:kaocha/plugins config) plugin))
          cli-options                                               (plugin/run-hook* plugin-chain :kaocha.hooks/cli-options cli-options)

          {:keys [errors options summary arguments]}                (cli/parse-opts args cli-options)
          ;; Final configuration load once all plugins are loaded:
          config                                                    (-> (config/load-config config-file (if profile {:profile profile} {}))
                                                                        (config/apply-cli options (map parse-kw arguments))
                                                                        (config/validate!))
          suites                                                    (into #{} (map parse-kw) arguments)]
      (plugin/with-plugins plugin-chain
        (run {:config  config
              :options options
              :errors  errors
              :suites  suites
              :summary (str "USAGE:\n\n"
                            "bin/kaocha [OPTIONS]... [TEST-SUITE]...\n\n"
                            summary
                            "\n\nOptions may be repeated multiple times for a logical OR effect.")})))))

(defn -main [& args]
  (try+
   (System/exit (apply -main* args))
   (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
     (System/exit exit-code))))

(defn exec-fn
  "Entry point for use with deps.tools' -X feature."
  [m]
  (try+
   (let [config (-> (config/load-config)
                    (config/merge-config (config/normalize m))
                    (config/validate!))]
     (if (:kaocha/watch? config)
       (let [[exit-code finish!] ((jit kaocha.watch/run) config)]
         (System/exit @exit-code))

       (plugin/with-plugins (plugin/load-all (:kaocha/plugins config))
         (let [config (plugin/run-hook :kaocha.hooks/config config)]
           (cond
             (:print-config config)
             (binding [clojure.core/*print-namespace-maps* false]
               (pprint/pprint config))

             (:print-test-plan config)
             (binding [clojure.core/*print-namespace-maps* false]
               (pprint/pprint (api/test-plan config)))

             :else
             (with-bindings (config/binding-map config)
               (plugin/run-hook :kaocha.hooks/main config)
               (let [result (plugin/run-hook :kaocha.hooks/post-summary (api/run config))
                     totals (result/totals (:kaocha.result/tests result))
                     exit-code (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255)]
                 (System/exit exit-code))))))))
   (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
     (System/exit exit-code))))

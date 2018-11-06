(ns kaocha.runner
  "Main entry point for command line use."
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [kaocha.config :as config]
            [kaocha.watch :as watch]
            [kaocha.output :as output]
            [kaocha.api :as api]
            [slingshot.slingshot :refer [try+]]
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

(defn help [summary]
  [""
   "USAGE:"
   ""
   (format "clj -m %s [OPTIONS]... [TEST-SUITE]..." (namespace `_))
   ""
   summary
   ""
   "Options may be repeated multiple times for a logical OR effect."])

(defn print-help! [summary]
  (println (str/join "\n" (help summary))))

(defn load-props [file-name]
  (with-open [^java.io.Reader reader (io/reader file-name)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) v])))))

(defn kaocha-version []
  (let [resource (io/resource "META-INF/maven/lambdaisland/kaocha/pom.properties")]
    (str "lambdaisland/kaocha " (pr-str (some-> resource load-props :version)))))

(defn- -main* [& args]
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
          suites                                     (into #{} (map :kaocha.testable/id) (:kaocha/tests config))
          unknown-suites                             (set/difference (into #{} (map parse-kw) arguments) (set suites))]

      (plugin/with-plugins plugin-chain
        (cond
          (seq errors)
          (do
            (run! println errors)
            (print-help! summary)
            -1)

          (or (:help options) (:test-help options))
          (do (print-help! summary) 0)

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
                          (str/join ", " (sort suites))
                          "."))
            -2)

          (:kaocha/watch? config)
          (do (watch/run config) 1) ; exit 1 because only an anomaly would break this loop

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
              (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255))))))))

(defn- exit-process! [code]
  (System/exit code))

(defn -main [& args]
  (try+
   (exit-process! (apply -main* args))
   (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
     (exit-process! exit-code))))

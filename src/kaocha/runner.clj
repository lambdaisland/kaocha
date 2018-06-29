(ns kaocha.runner
  "Main entry point for command line use."
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [kaocha.config2 :as config]
            [kaocha.watch :as watch]
            [kaocha.output :as output]
            [kaocha.test :as test]
            [kaocha.api :as api]
            [slingshot.slingshot :refer [try+]]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]))

(defn- accumulate [m k v]
  (update m k (fnil conj []) v))

(def ^:private cli-options
  [["-c" "--config-file FILE"   "Config file to read."
    :default "tests.edn"]
   #_[nil  "--print-config"       "Print out the fully merged and normalized config, then exit."]
   [nil  "--fail-fast"          "Stop testing after the first failure."]
   #_[nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."]
   #_[nil  "--[no-]watch"         "Watch filesystem for changes and re-run tests."]
   #_[nil  "--reporter SYMBOL"    "Change the test reporter, can be specified multiple times."
      :parse-fn symbol
      :assoc-fn accumulate
      :default-desc (str (:reporter (config/default-config)))]
   #_[nil  "--source-path PATH"   "Path containing code under test."
      :assoc-fn accumulate
      :default-desc (str (first (:source-paths (config/default-config))))]
   #_[nil  "--test-path PATH"     "Path to scan for test namespaces."
      :assoc-fn accumulate
      :default-desc (str (first (:test-paths (config/default-config))))]
   #_[nil  "--ns-pattern PATTERN" "Regexp pattern to identify test namespaces."
      :assoc-fn accumulate
      :parse-fn #(java.util.regex.Pattern/compile %)
      :default-desc (str (first (:ns-patterns (config/default-config))))]
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

(defn- -main* [& args]
  (let [{{:keys [config-file]} :options}           (cli/parse-opts args cli-options)
        config                                     (config/load-config (or config-file "tests.edn"))
        plugin-chain                               (plugin/load-all (:kaocha/plugins config))
        cli-options                                (plugin/run-hook plugin-chain :kaocha.hooks/cli-options cli-options)
        {:keys [errors options arguments summary]} (cli/parse-opts args cli-options)
        config                                     (-> config
                                                       (config/apply-cli-opts options)
                                                       (config/apply-cli-args arguments))]

    (cond
      (seq errors)
      (do
        (run! println errors)
        (print-help! summary)
        -1)

      (:test-help options)
      (do
        (print-help! summary)
        0)

      :else
      (let [result (api/run config)
            totals (result/totals result)]
        (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255)))))

(defn- exit-process! [code]
  (System/exit code))

(defn -main [& args]
  (try+
   (exit-process! (apply -main* args))
   (catch :kaocha/reporter-not-found {:kaocha/keys [reporter-not-found]}
     (output/error "Failed to resolve reporter var: " reporter-not-found)
     (exit-process! -3))))



#_
(let [config         (config options)
      normalized     config #_(config/normalize config)
      valid-suites   (into #{} (map :kaocha/id) (:kaocha/suites normalized))
      unknown-suites (set/difference (:only-suites options) valid-suites)]
  (cond
    (:print-config options)
    (do
      (pprint/pprint normalized)
      0)

    (seq unknown-suites)
    (do
      (println (format "No such suite: %s, valid options: %s."
                       (str/join ", " (sort unknown-suites))
                       (str/join ", " (sort valid-suites))))
      -2)

    :else
    (if (:kaocha/watch? normalized)
      (watch/run config)
      (let [{:keys [fail error] :or {fail 0 error 0}} (test/run config)]
        (mod (+ fail error) 255)))))

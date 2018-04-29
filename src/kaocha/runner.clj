(ns kaocha.runner
  "Main entry point for command line use."
  (:gen-class)
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [kaocha :as k]
            [kaocha.config :as config]
            [kaocha.watch :as watch]
            [kaocha.output :as output]
            [kaocha.test :as test]
            [slingshot.slingshot :refer [try+]]))

(defn- accumulate [m k v]
  (update m k (fnil conj []) v))

(def ^:private cli-options
  [["-c" "--config-file FILE"   "Config file to read."
    :default "tests.edn"]
   [nil  "--print-config"       "Print out the fully merged and normalized config, then exit."]
   [nil  "--fail-fast"          "Stop testing after the first failure."]
   [nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."]
   [nil  "--[no-]watch"         "Watch filesystem for changes and re-run tests."]
   [nil  "--[no-]randomize"     "Run test namespaces and vars in random order."]
   [nil  "--seed SEED"          "Provide a seed to determine the random order of tests."
    :parse-fn #(Integer/parseInt %)]
   [nil  "--reporter SYMBOL"    "Change the test reporter, can be specified multiple times."
    :parse-fn symbol
    :assoc-fn accumulate
    :default-desc (str (:reporter (config/default-config)))]
   [nil  "--source-path PATH"   "Path containing code under test."
    :assoc-fn accumulate
    :default-desc (str (first (:source-paths (config/default-config))))]
   [nil  "--test-path PATH"     "Path to scan for test namespaces."
    :assoc-fn accumulate
    :default-desc (str (first (:test-paths (config/default-config))))]
   [nil  "--ns-pattern PATTERN" "Regexp pattern to identify test namespaces."
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

(defn config [options]
  (let [{:keys [config-file] :as options} (config/normalize-cli-opts options)
        config (config/load-config (or config-file "tests.edn"))]
    (merge config options)))

(defn- -main* [& args]
  (let [{:keys [errors options arguments summary]} (cli/parse-opts args cli-options)
        options                                    (cond-> options
                                                     (seq arguments)
                                                     (assoc :only-suites (into #{} (map keyword) arguments)))]

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
      (let [config         (config options)
            normalized     (config/normalize config)
            valid-suites   (into #{} (map :id) (:suites normalized))
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
          (if (:watch normalized)
            (watch/run config)
            (let [{:keys [fail error] :or {fail 0 error 0}} (test/run config)]
              (mod (+ fail error) 255))))))))

(defn- exit-process! [code]
  (System/exit code))

(defn -main [& args]
  (try+
   (exit-process! (apply -main* args))
   (catch ::k/reporter-not-found {::k/keys [reporter-not-found]}
     (output/error "Failed to resolve reporter var: " reporter-not-found)
     (exit-process! -3))))

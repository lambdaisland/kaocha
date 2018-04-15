(ns lambdaisland.kaocha.runner
  "Main entry point for command line use."
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.test]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]
            [lambdaisland.kaocha.config :as config]
            [lambdaisland.kaocha.output :as output]
            [lambdaisland.kaocha.test :as test]
            [clojure.set :as set]))

(defn- accumulate [m k v]
  (update m k (fnil conj []) v))

(def ^:private cli-options
  [["-c" "--config-file FILE"   "Config file to read"
    :default "tests.edn"]
   [nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."]
   [nil  "--print-config"       "Print out the fully merged and normalized config, then exit."]
   [nil  "--fail-fast"          "Stop testing after the first failure."]
   [nil  "--reporter SYMBOL"
    :parse-fn symbol
    :assoc-fn accumulate]
   [nil  "--test-path PATH"     "Path to scan for test namespaces"
    :assoc-fn accumulate]
   [nil  "--ns-pattern PATTERN" "Regexp pattern to identify test namespaces"
    :assoc-fn accumulate
    :parse-fn #(java.util.regex.Pattern/compile %)]
   ["-H" "--test-help"          "Display this help message"]])

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
    (merge
     (config/default-config)
     config
     options)))

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
          (let [{:keys [fail error] :or {fail 0 error 0}} (test/run config)]
            (mod (+ fail error) 255)))))))

(defn- exit-process! [code]
  (System/exit code))

(defn -main [& args]
  (exit-process! (apply -main* args)))

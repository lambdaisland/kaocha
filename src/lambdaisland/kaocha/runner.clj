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
            [lambdaisland.kaocha.test :as test]))

(defn- accumulate [m k v]
  (update m k (fnil conj []) v))
(defn- parse-kw
  [s]
  (if (.startsWith s ":") (read-string s) (keyword s)))

(def ^:private cli-options
  [["-c" "--config-file FILE"   "Config file to read"
    :default "tests.edn"]
   [nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."]
   [nil  "--print-config" "Print out the fully merged and normalized config, then exit."]
   [nil  "--reporter SYMBOL"
    :parse-fn symbol]
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

(defn- exit-process! [code]
  (System/exit code))

(defn config [options]
  (let [{:keys [config-file] :as options} (config/normalize-cli-opts options)
        config (config/load-config (or config-file "tests.edn"))]
    (merge
     (config/default-config)
     config
     options)))

(defn- -main* [& args]
  (let [{:keys [errors options arguments summary]} (cli/parse-opts args cli-options)
        options (cond-> options
                  (seq arguments)
                  (assoc :only-suites arguments))]

    (cond
      (seq errors)
      (do
        (run! println errors)
        (print-help! summary)
        1)

      (:test-help options)
      (do
        (print-help! summary)
        0)

      (:print-config options)
      (do
        (-> options
            config
            config/normalize
            pprint/pprint)
        0)

      :else
      (let [{:keys [fail error] :or {fail 0 error 0}} (-> options
                                                          config
                                                          test/run-suites)]
        (mod (+ fail error) 255)))))

(defn -main [& args]
  (exit-process! (apply -main* args)))

;; the color setting from the config file isn't honored for the summary
;; implement our own var finding so we can easily detect if no vars are found, and report that.
;; override reporter on command line

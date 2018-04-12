(ns lambdaisland.kaocha.runner
  "Main entry point for command line use."
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.test]
            [clojure.tools.namespace.find :as ctn.find]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]
            [lambdaisland.kaocha.config :as config]
            [lambdaisland.kaocha.output :as output]
            [lambdaisland.kaocha.test :as test]
            [lambdaisland.kaocha.output :as out]))

(defn- accumulate-vector [m k v]
  (update m k (fnil conj []) v))

(defn- accumulate [m k v]
  (update m k (fnil conj #{}) v))

(defn- parse-kw
  [s]
  (if (.startsWith s ":") (read-string s) (keyword s)))

(def ^:private cli-options
  [["-c" "--config-file FILE"   "Config file to read"
    :default "tests.edn"]
   [nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."]
   [nil  "--print-config SUITE" "Print out the fully merged configuration for the given suite, then exit."]
   [nil  "--test-path PATH"     "Path to scan for test namespaces"
    :assoc-fn accumulate-vector]
   [nil  "--ns-pattern PATTERN" "Regexp pattern to identify test namespaces"
    :assoc-fn accumulate-vector
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

(defn- resolve-reporter [reporter]
  (cond
    (symbol? reporter)
    (do
      (require (symbol (namespace reporter)))
      (resolve reporter))

    (seq? reporter)
    (let [rs (map resolve-reporter reporter)]
      (fn [m] (run! #(% m) rs)))))

(defn runner [{:keys [config-file] :as options} suite-ids]
  (binding [output/*colored-output* (:color options true)]
    (let [config          (config/load-config config-file)
          reporter        (resolve-reporter (:reporter config))
          suite-config-fn #(-> (merge (dissoc config :suites) %)
                               (config/merge-options options))
          suites          (cond->> (map suite-config-fn (:suites config))
                            (seq suite-ids)
                            (filter #(some #{(name (:id %))} suite-ids)))]
      (when-let [suite-name (:print-config options)]
        (if-let [suite (some #(and (= (name (:id %)) suite-name) %) suites)]
          (pprint/pprint (dissoc suite :config-file :print-config))
          (out/warn "--print-config " suite-name ": no such suite."))
        (exit-process! 0))
      (test/run-suites reporter suites))))

(defn -main [& args]
  (let [{:keys [errors options arguments summary]} (cli/parse-opts args cli-options)]

    (if (seq errors)
      (do
        (run! println errors)
        (print-help! summary)
        (exit-process! 1))

      (if (:test-help options)
        (print-help! summary)
        (let [{:keys [fail error] :or {fail 0 error 0}} (runner options arguments)]
          (exit-process! (mod (+ fail error) 255)))))))

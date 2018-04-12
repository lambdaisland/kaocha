(ns lambdaisland.kaocha.runner
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.test]
            [clojure.tools.namespace.find :as ctn.find]
            [clojure.java.io :as io]
            [lambdaisland.kaocha.load :as load]
            [lambdaisland.kaocha.config :as config]
            [lambdaisland.kaocha.output :as output]))

(defn- run-tests [{:keys [color] :as suite}]
  (binding [output/*colored-output* color]
    (apply clojure.test/run-tests (load/load-tests suite))))

(defn- accumulate [m k v]
  (update m k (fnil conj #{}) v))

(defn- parse-kw
  [s]
  (if (.startsWith s ":") (read-string s) (keyword s)))

(def ^:private cli-options
  [["-c" "--config-file FILE"   "Config file to read"
    :default "tests.edn"]
   [nil  "--[no-]color"         "Enable/disable ANSI color codes in output. Defaults to true."
    :default true]
   [nil  "--test-path PATH"     "Path to scan for test namespaces"
    :assoc-fn accumulate]
   [nil  "--ns-pattern PATTERN" "Regexp pattern to identify test namespaces"
    :assoc-fn accumulate]
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

(defn runner [{:keys [config-file] :as options} suite-ids]
  (binding [output/*colored-output* (:color options)]
    (let [config (config/load-config config-file)
          suites (map #(-> %
                           (merge (dissoc config :suites))
                           (config/merge-options options))
                      (:suites config))]
      (apply merge-with #(if (int? %1) (+ %1 %2) %2)
             (map run-tests (if (seq suite-ids)
                              (filter #(some #{(name (:id %))} suite-ids) suites)
                              suites))
             ))))

(defn -main [& args]
  (let [{:keys [errors options arguments summary]} (cli/parse-opts args cli-options)]

    (if (seq errors)
      (do
        (run! println errors)
        (print-help! summary)
        (exit-process! 1))

      (if (:test-help options)
        (print-help! summary)
        (let [{:keys [fail error]} (runner options arguments)]
          (exit-process! (mod (+ fail error) 255)))))))

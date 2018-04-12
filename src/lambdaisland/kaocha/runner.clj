(ns lambdaisland.kaocha.runner
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.test]
            [clojure.tools.namespace.find :as ctn.find]
            [lambdaisland.kaocha.classpath :as cp]))

(defn run-tests []
  (cp/maybe-add-dynamic-classloader)
  (cp/add-classpath "test")
  (require 'lambdaisland.kaocha.runner-test)
  (clojure.test/run-tests (find-ns 'lambdaisland.kaocha.runner-test)))

(def cli-options
  [["-H" "--test-help" "Display this help message"]])

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

(defn -main [& args]
  (let [{:keys [errors options summary]} (cli/parse-opts args cli-options)]

    (if (seq errors)
      (do
        (run! println errors)
        (print-help! summary)
        (exit-process! 1))

      (if (:test-help options)
        (print-help! summary)
        (run-tests)))))

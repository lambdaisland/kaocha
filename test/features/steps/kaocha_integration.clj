(ns features.steps.kaocha-integration
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [lambdaisland.cucumber.dsl :refer :all]
            [kaocha.report :as report]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.output :as output])
  (:import [java.nio.file Files OpenOption Path Paths]
           java.nio.file.attribute.FileAttribute))

(require 'kaocha.assertions)

(defprotocol Joinable
  (join [this that]))

(extend-protocol Joinable
  String
  (join [this that] (join (Paths/get this (make-array String 0)) that))
  Path
  (join [this that] (.resolve this (str that))))

(extend-protocol io/IOFactory
  Path
  (make-output-stream [this opts]
    (Files/newOutputStream this (into-array OpenOption [])))
  (make-input-stream [this opts]
    (Files/newInputStream this (into-array OpenOption [])))
  (make-writer [this opts]
    (io/make-writer (io/make-output-stream this opts) opts))
  (make-reader [this opts]
    (io/make-reader (io/make-input-stream this opts) opts)))

(extend-protocol io/Coercions
  Path
  (as-file [path] (.toFile path))
  (as-url [path] (.toURL (.toFile path))))

(def shellwords-pattern #"[^\s'\"]+|[']([^']*)[']|[\"]([^\"]*)[\"]")

;; ported from cucumber.runtime.ShellWords, which was ported from Ruby
(defn shellwords [cmdline]
  (let [matcher (re-matcher shellwords-pattern cmdline)]
    (loop [res []]
      (if (.find matcher)
        (recur
         (if-let [word (.group matcher 1)]
           (conj res word)
           (let [word (.group matcher)]
             (if (and (= \" (first word))
                      (= \" (last word)))
               (conj res (subs word 1 (dec (count word))))
               (conj res word)))))
        res))))

(def default-attributes (into-array FileAttribute []))

(defn temp-dir []
  (Files/createTempDirectory "kaocha_integration" default-attributes))

(defn mkdir [path]
  (Files/createDirectories path default-attributes))

(Given "the default test configuration" [m]
  (let [dir (temp-dir)
        test-dir (join dir "test")
        config-file (join dir "tests.edn")]
    (spit (str config-file) (str "#kaocha/v1\n"
                                 "{:tests [{:test-paths [\""
                                 test-dir
                                 "\"]}]\n"
                                 ":color? false\n"
                                 ":randomize? false}"))
    (mkdir test-dir)
    (assoc m
           :dir dir
           :test-dir test-dir
           :config-file config-file)))

(defn ns->fname [ns]
  (-> ns
      str
      (str/replace #"\." "/")
      (str/replace #"-" "_")
      (str ".clj")))

(Given "the following test namespace" [{:keys [test-dir] :as m} contents]
  (let [ns-form (read-string contents)]
    (assert (= 'ns (and (seq ns-form) (first ns-form))))
    (assert (symbol? (second ns-form)))
    (let [fname (join test-dir (ns->fname (second ns-form)))]
      (mkdir (.getParent fname))
      (spit fname contents)))
  m)

(Given "the following test configuration" [m doc-string]
  (let [dir (temp-dir)
        test-dir (join dir "test")
        config-file (join dir "tests.edn")]
    (spit (str config-file) doc-string)
    (mkdir test-dir)
    (assoc m
           :dir dir
           :config-file config-file)))

(Given "the file {string} containing" [{:keys [dir] :as m} path contents]
  (let [path (join dir path)]
    (mkdir (.getParent path))
    (spit path contents)
    m))


(When "I run Kaocha with {string}" [{:keys [config-file dir] :as m} args]
  (let [args   (into ["clojure"
                      "-Sdeps" (str "{:deps {lambdaisland/kaocha {:local/root \""
                                    (.getAbsolutePath (io/file ""))
                                    "\"}}}")
                      "-m" "kaocha.runner"
                      "--config-file" (str config-file)]
                     (shellwords args))
        result (apply shell/sh (conj args :dir dir))]
    ;; By default these are hidden unless the test fails
    (when (seq (:out result))
      (println (str dir) "$" (str/join " " (map (fn [a]
                                                  (if (str/includes? a " ") (str "'" a "'") a))
                                                args)))
      (println (str (output/colored :underline "stdout") ":\n" (:out result))))
    (when (seq (:err result))
      (println (str (output/colored :underline "stderr") ":\n" (:err result))))
    (merge m result)))

(Then "the exit-code is non-zero" [{:keys [exit] :as m}]
  (is (not= "0" exit))
  m)

(Then "the exit-code should be {int}" [{:keys [exit] :as m} code]
  (is (= code (Integer. exit)))
  m)

(Then "the output should contain" [m output]
  (is (substring? output (:out m)))
  m)

(Then "the output should not contain" [m output]
  (is (not (str/includes? (:out m) output)))
  m)

(Then "stderr should contain" [m output]

  (is (substring? output (:err m)))
  m)

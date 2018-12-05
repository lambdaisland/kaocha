(ns features.steps.kaocha-integration
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [kaocha.output :as output]
            [lambdaisland.cucumber.dsl :refer :all]
            [me.raynes.fs :as fs])
  (:import java.io.File
           [java.nio.file Files OpenOption Path Paths]
           java.nio.file.attribute.FileAttribute))

(require 'kaocha.assertions)

(defprotocol Joinable
  (join [this that]))

(extend-protocol Joinable
  String
  (join [this that] (join (Paths/get this (make-array String 0)) that))
  Path
  (join [this that] (.resolve this (str that)))
  File
  (join [this that] (.toPath (io/file this (str that)))))

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

(defn ns->fname [ns]
  (-> ns
      str
      (str/replace #"\." "/")
      (str/replace #"-" "_")
      (str ".clj")))

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

(Given "the following test namespace" [{:keys [test-dir] :as m} contents]
  (let [ns-form (read-string contents)]
    (assert (= 'ns (and (seq ns-form) (first ns-form))))
    (assert (symbol? (second ns-form)))
    (let [fname (join test-dir (ns->fname (second ns-form)))]
      (mkdir (.getParent fname))
      (spit fname contents)))
  m)

(Given "shared test fixtures" [m]
  (assoc m :copy-fixtures? true))

(Given "the following test configuration" [m doc-string]
  (let [dir (temp-dir)
        test-dir (join dir "test")
        config-file (join dir "tests.edn")]
    (spit (str config-file) doc-string)
    (mkdir test-dir)
    (when (:copy-fixtures? m)
      (fs/copy-dir (io/file "fixtures") dir))
    (assoc m
           :dir dir
           :config-file config-file)))

(Given "the file {string} containing" [{:keys [dir] :as m} path contents]
  (let [path (join dir path)]
    (mkdir (.getParent path))
    (spit path contents)
    m))

(defn project-dir-path [& paths]
  (str (reduce join (.getAbsolutePath (io/file "")) paths)))

(defn codecov? []
  (= (System/getenv "CI") "true"))

(When "I run Kaocha with {string}" [{:keys [config-file dir] :as m} args]
      (let [args (cond-> ["clojure"
                          "-Sdeps" (str "{:deps {lambdaisland/kaocha {:local/root \""
                                        (project-dir-path)
                                        "\"}"
                                        "lambdaisland/kaocha-cloverage {:mvn/version \"RELEASE\"}}}")
                          "-m" "kaocha.runner"
                          "--config-file" (str config-file)]
                   (codecov?)
                   (into ["--plugin" "cloverage"
                          "--cov-output" (project-dir-path "target/coverage" (str (gensym "integration")))
                          "--cov-src-ns-path" (project-dir-path "src")
                          "--codecov"])
                   :always
                   (into (shellwords args)))

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

(Then "the output should be" [m output]
  (is (= (str/trim output) (str/trim (:out m))))
  m)

(Then "the output should not contain" [m output]
  (is (not (str/includes? (:out m) output)))
  m)

(Then "stderr should contain" [m output]
  (is (substring? output (:err m)))
  m)

#_
(do
  (require 'kaocha.repl)
  (kaocha.repl/run "integration"))

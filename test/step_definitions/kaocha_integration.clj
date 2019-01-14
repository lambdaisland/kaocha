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
           [java.nio.file.attribute FileAttribute PosixFilePermissions]))

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

(defn temp-dir
  ([]
   (temp-dir "kaocha_integration"))
  ([path]
   (Files/createTempDirectory path default-attributes)))

(defonce clj-cpcache-dir (temp-dir "kaocha_cpcache"))

(defn mkdir [path]
  (Files/createDirectories path default-attributes))

(defn codecov? []
  (= (System/getenv "CI") "true"))

(defn project-dir-path [& paths]
  (str (reduce join (.getAbsolutePath (io/file "")) paths)))

(defmacro with-print-namespace-maps [bool & body]
  (if (find-var 'clojure.core/*print-namespace-maps*)
    `(binding [*print-namespace-maps* ~bool]
       ~@body)
    ;; pre Clojure 1.9
    `(do ~@body)))

(defn write-deps-edn [path]
  (with-open [deps-out (io/writer path)]
    (binding [*out* deps-out]
      (with-print-namespace-maps false
        (clojure.pprint/pprint {:deps {'lambdaisland/kaocha           {:local/root (project-dir-path)}
                                       'lambdaisland/kaocha-cloverage {:mvn/version "RELEASE"}}})))))

(defn test-dir-setup [m]
  (if (:dir m)
    m
    (let [dir         (temp-dir)
          test-dir    (join dir "test")
          bin-dir     (join dir "bin")
          config-file (join dir "tests.edn")
          deps-edn    (join dir "deps.edn")
          runner      (join dir "bin/kaocha")]
      (mkdir test-dir)
      (mkdir bin-dir)
      (spit (str config-file)
            (str "#kaocha/v1\n"
                 "{:color? false\n"
                 " :randomize? false}"))
      (spit (str runner)
            (str/join " "
                      (cond-> ["clojure"
                               "-m" "kaocha.runner"]
                        (codecov?)
                        (into ["--plugin" "cloverage"
                               "--cov-output" (project-dir-path "target/coverage" (str (gensym "integration")))
                               "--cov-src-ns-path" (project-dir-path "src")
                               "--codecov"])
                        :always
                        (conj "\"$@\""))))
      (write-deps-edn deps-edn)
      (Files/setPosixFilePermissions runner (PosixFilePermissions/fromString "rwxr--r--"));
      (assoc m
             :dir dir
             :test-dir test-dir
             :config-file config-file
             :runner runner))))

(defn ns->fname [ns]
  (-> ns
      str
      (str/replace #"\." "/")
      (str/replace #"-" "_")
      (str ".clj")))

(Given "a file named {string} with:" [m path contents]
  (let [{:keys [dir] :as m} (test-dir-setup m)
        path (join dir path)]
    (mkdir (.getParent path))
    (spit path contents)
    m))

(def last-cpcache-dir (atom nil))

(When "I run `(.*)`" [m args]
  (let [{:keys [config-file dir] :as m} (test-dir-setup m)]

    (when-let [cache @last-cpcache-dir]
      (let [target (join dir ".cpcache")]
        (mkdir target)
        (run! #(fs/copy % (io/file (join target (.getName %)))) (fs/glob cache "*"))))

    (let [result (apply shell/sh (conj (shellwords args)
                                       :dir dir))]
      ;; By default these are hidden unless the test fails
      (when (seq (:out result))
        (println (str dir) "$" args)
        (println (str (output/colored :underline "stdout") ":\n" (:out result))))
      (when (seq (:err result))
        (println (str (output/colored :underline "stderr") ":\n" (:err result))))
      (let [cpcache (io/file (join dir ".cpcache"))]
        (when (.exists cpcache)
          (reset! last-cpcache-dir cpcache)))
      (merge m result))))

(Then "the exit-code is non-zero" [{:keys [exit] :as m}]
  (is (not= "0" exit))
  m)

(Then "the exit-code should be {int}" [{:keys [exit] :as m} code]
  (is (= code (Integer. exit)))
  m)

(Then "the output should contain:" [m output]
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

(Then "print output" [m]
  (println "----out---------------------------------------")
  (println (:out m))
  (println "----err---------------------------------------")
  (println (:err m))
  m)

#_
(do
  (require 'kaocha.repl)
  (kaocha.repl/run :plugins.hooks-plugin {:kaocha.plugin.capture-output/capture-output? false
                                          }
    ))

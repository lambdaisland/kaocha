(ns kaocha.integration-helpers
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
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

(defn spit-file [m path contents]
  (let [{:keys [dir] :as m} (test-dir-setup m)
        path (join dir path)]
    (mkdir (.getParent path))
    (spit path contents)
    m))

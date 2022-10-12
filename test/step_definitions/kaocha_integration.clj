(ns features.steps.kaocha-integration
  ^{:clojure.tools.namespace.repl/load false
    :clojure.tools.namespace.repl/unload false}
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :as t :refer :all]
            [kaocha.integration-helpers :refer :all]
            [kaocha.output :as output]
            [kaocha.shellwords :refer [shellwords]]
            [lambdaisland.cucumber.dsl :refer :all]
            [me.raynes.fs :as fs]))

(require 'kaocha.assertions)

(Given "a file named {string} with:" [m path contents]
  (spit-file m path contents))

(def last-cpcache-dir (atom nil))

(When "I run `(.*)`" [m args]
  (let [{:keys [config-file dir] :as m} (test-dir-setup m)]

    (when-let [cache @last-cpcache-dir]
      (let [target (join dir ".cpcache")]
        (when-not (.isDirectory (io/file target))
          (mkdir target)
          (run! #(fs/copy % (io/file (join target (.getName %)))) (fs/glob cache "*")))))

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

(Then "stderr should contain:" [m output]
  (is (substring? output (:err m)))
  m)

(Then "the output should be" [m output]
  (is (= (str/trim output) (str/trim (:out m))))
  m)

(Then "the output should not contain" [m output]
  (is (not (str/includes? (:out m) output)))
  m)

(Then "the EDN output should contain:" [m output]
      (let  [actual (edn/read-string (:out m))
             expected (edn/read-string output)]
        (is (= (type actual) (type expected)))
        (is (= (select-keys actual (keys expected)) expected)))
      m)


(Then "stderr should contain" [m output]
  (is (substring? output (:err m)))
  m)

(Then "print output" [m]
  (t/with-test-out
    (println "----out---------------------------------------")
    (println (:out m))
    (println "----err---------------------------------------")
    (println (:err m)))
  m)

#_
(do
  (require 'kaocha.repl)
  (kaocha.repl/run :plugins.version-filter {:kaocha.plugin.capture-output/capture-output? false
                                            }
    ))

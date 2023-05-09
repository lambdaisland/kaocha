(ns kaocha.canary 
  "Babashka script for checking out and running test suites"
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]))

(def github-clone-url "https://github.com")

(def test-suites [{:repo-name "lambdaisland/deep-diff2"
                    :suite "clj"}
                  {:repo-name "lambdaisland/regal"
                   :suite "clj"}])


(def current-wd (System/getProperty "user.dir")) 


(let [temp-dir (fs/create-temp-dir)]
  (doseq [{:keys [repo-name suite]} test-suites ]
    (println "Testing " repo-name)
    ;; (prn suite)
    (let [repo-dir (str temp-dir "/" repo-name) ]
      (shell (format "git clone %s %s" (str github-clone-url "/" repo-name) repo-dir))
      (shell (format "echo %s" current-wd))
      (shell {:dir repo-dir}
             (format "clojure -A:test -Sdeps '{:deps {lambdaisland/kaocha {:local/root \"%s/\"}}}' -M -m kaocha.runner %s" current-wd suite)))))

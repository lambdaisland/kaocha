(ns kaocha.canary 
  "Babashka script for checking out and running test suites"
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]))

(def github-clone-url "https://github.com")

(def test-suites [{:repo-name "lambdaisland/deep-diff2"
                    :suite "clj"}
                  {:repo-name "lambdaisland/regal"
                   :suite "clj"}
                  {:repo-name "lambdaisland/uri"
                   :suite "clj"}
                  {:repo-name "lambdaisland/kaocha-cucumber"
                   :suite "unit"}
                  {:repo-name "lambdaisland/kaocha-doctest"
                   :suite "unit"}
                  {:repo-name "lambdaisland/kaocha-cloverage"
                   :suite ""}
                  {:repo-name "lambdaisland/kaocha-junit-xml"
                   :suite ""} ])

(def current-wd (System/getProperty "user.dir")) 


(let [temp-dir (fs/create-temp-dir)]
  (doseq [{:keys [repo-name suite]} test-suites ]
    (println "Testing " repo-name)
    (let [repo-dir (str temp-dir "/" repo-name) ]
      (shell (format "git clone %s %s" (str github-clone-url "/" repo-name) repo-dir))
      ;; (shell {:dir repo-dir}
      ;;   (format "clojure -Sdeps '{:aliases {:test-local {:override-deps {lambdaisland/kaocha {:local/root \"%s/\"}}}}}' -A:test:test-local -Stree" current-wd))
      (shell {:dir repo-dir}
             (format "clojure -Sdeps '{:aliases {:test-local {:override-deps {lambdaisland/kaocha {:local/root \"%s/\"}}}}}' -A:test:test-local -m kaocha.runner %s" current-wd suite)))))

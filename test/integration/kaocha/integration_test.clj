(ns kaocha.integration-test
  (:require [kaocha.test-util]
            [clojure.test :refer :all]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]))

(defn invoke-runner [& args]
  (apply shell/sh "clojure" "-m" "kaocha.runner" "--no-randomize" args))

(defn invoke-with-config [config & args]
  (let [tmpfile (java.io.File/createTempFile "tests" ".edn")]
    (doto tmpfile
      (.deleteOnExit)
      (spit (prn-str config)))
    (apply shell/sh
           "clojure" "-m" "kaocha.runner"
           "--config-file" (str tmpfile)
           "--no-randomize"
           args)))

(deftest command-line-runner-test
  (testing "it lets you specifiy the test suite name"
    (is (match? {:exit 0
                 :out  ".\n1 test vars, 1 assertions, 0 failures.\n"
                 :err  ""}
                (invoke-runner "--no-color" "--config-file" "fixtures/tests.edn" "a"))))

  (testing "it can print the config"
    (is (match? (-> (invoke-with-config #:kaocha{:suites [#:kaocha{:id          :aaa
                                                                   :test-paths  ["fixtures/a-tests"]
                                                                   :ns-patterns ["^foo$"]}]}
                                        "--print-config")
                    (update :out read-string))
                {:exit 0,
                 :out  #:kaocha{:color?     true
                                :randomize? false
                                :watch?     false
                                :suites     [#:kaocha{:ns-patterns  ["^foo$"]
                                                      :source-paths ["src"]
                                                      :test-paths   ["fixtures/a-tests"]
                                                      :id           :aaa}]
                                :reporter   'kaocha.report/progress}
                 :err  ""})))

  (testing "it elegantly reports when no tests are found"
    (is (match? (invoke-with-config #:kaocha{:color?  false
                                             :suites
                                             [#:kaocha{:id          :empty
                                                       :test-paths  ["fixtures/a-tests"]
                                                       :ns-patterns [#"^foo$"]}]})
                {:exit 0, :out "\n0 test vars, 0 assertions, 0 failures.\n", :err ""})))

  (testing "--fail-fast"
    (is (match? {:err  ""
                 :out  (str ".\n.F\n\n"
                            "FAIL in (fail-1) (hello_test.clj:11)\n"
                            "expected: false\n"
                            "  actual: false\n"
                            "3 test vars, 3 assertions, 1 failures.\n")
                 :exit 1}
                (invoke-runner "--config-file" "fixtures/with_failing.edn" "--no-color" "--fail-fast"))))

  (testing "Invalid suite"
    (is (match? {:err  ""
                 :out  "No such suite: :foo, valid options: :a, :b.\n"
                 :exit 254}
                (invoke-runner "--config-file" "fixtures/tests.edn" "--no-color" "foo"))))

  (testing "Invalid reporter"
    (is (match? {:exit 253
                 :out  ""
                 :err  "\u001b[31mERROR: \u001b[0mFailed to resolve reporter var: kaocha/does-not-exist\n"}
                (invoke-runner "--reporter" "kaocha/does-not-exist")))

    (is (match? {:exit 253
                 :out  ""
                 :err  "\u001b[31mERROR: \u001b[0mFailed to resolve reporter var: does/not-exist\n"}
                (invoke-runner "--reporter" "does/not-exist")))))

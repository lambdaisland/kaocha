(ns lambdaisland.kaocha.integration-test
  (:require [clojure.test :refer :all]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn invoke-runner [& args]
  (apply shell/sh "clj" "-m" "lambdaisland.kaocha.runner" "--no-randomize" args))

(defn invoke-with-config [config & args]
  (let [tmpfile (java.io.File/createTempFile "tests" ".edn")]
    (doto tmpfile
      (.deleteOnExit)
      (spit (prn-str config)))
    (apply shell/sh
           "clj" "-m" "lambdaisland.kaocha.runner"
           "--config-file" (str tmpfile)
           "--no-randomize"
           args)))

(deftest command-line-runner-test
  (testing "it lets you specifiy the test suite name"
    (is (= {:exit 0
            :out ".\n1 test vars, 1 assertions, 0 failures.\n"
            :err ""}
           (invoke-runner "--no-color" "--config-file" "fixtures/tests.edn" "a"))))

  (testing "it can print the config"
    (is (= (-> (invoke-with-config {:suites [{:id :aaa
                                              :test-paths ["fixtures/a-tests"]
                                              :ns-patterns ["^foo$"]}]}
                                   "--print-config")
               (update :out read-string))
           {:exit 0,
            :out {:color true
                  :randomize false
                  :suites [{:ns-patterns ["^foo$"]
                            :test-paths ["fixtures/a-tests"]
                            :id :aaa}]
                  :reporter 'lambdaisland.kaocha.report/progress}
            :err ""})))

  (testing "it elegantly reports when no tests are found"
    (is (= (invoke-with-config {:color false
                                :suites [{:id :empty
                                          :test-paths ["fixtures/a-tests"]
                                          :ns-patterns [#"^foo$"]}]})
           {:exit 0, :out "\n0 test vars, 0 assertions, 0 failures.\n", :err ""})))


  (testing "--fail-fast"
    (is (= {:err ""
            :out (str ".\n.F\n\n"
                      "FAIL in (fail-1) (hello_test.clj:11)\n"
                      "expected: false\n"
                      "  actual: false\n"
                      "3 test vars, 3 assertions, 1 failures.\n")
            :exit 1}
           (invoke-runner "--config-file" "fixtures/with_failing.edn" "--no-color" "--fail-fast"))))

  (testing "Invalid suite"
    (is (= {:err ""
            :out "No such suite: :foo, valid options: :a, :b.\n"
            :exit 254}
           (invoke-runner "--config-file" "fixtures/tests.edn" "--no-color" "foo"))))

  (testing "Invalid reporter"
    (is (= {:exit 253
            :out ""
            :err "\u001b[31mERROR: \u001b[0mFailed to resolve reporter var: lambdaisland.kaocha/does-not-exist\n"}
           (invoke-runner "--reporter" "lambdaisland.kaocha/does-not-exist")))

    (is (= {:exit 253
            :out ""
            :err "\u001b[31mERROR: \u001b[0mFailed to resolve reporter var: does/not-exist\n"}
           (invoke-runner "--reporter" "does/not-exist"))) )

  )

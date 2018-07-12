(ns kaocha.integration-test
  (:require [kaocha.test-util]
            [clojure.test :refer :all]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [kaocha.config]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [kaocha.config :as config]))

(defn invoke-runner [& args]
  (apply shell/sh "clojure" "-m" "kaocha.runner" "--no-color" "--no-randomize" args))

(defn invoke-with-config [config & args]
  (let [tmpfile (java.io.File/createTempFile "tests" ".edn")]
    (doto tmpfile
      (.deleteOnExit)
      (spit (str "#kaocha" (prn-str config))))
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
                (invoke-runner "--config-file" "fixtures/tests.edn" "a"))))

  (testing "it can print the config"
    (is (match? {:exit 0
                 :out  '{:kaocha.plugin.randomize/randomize? false
                         :kaocha/reporter                    [kaocha.report/dots]
                         :kaocha/color?                      true
                         :kaocha/fail-fast?                  false
                         :kaocha/plugins                     [:kaocha.plugin/randomize
                                                              :kaocha.plugin/filter]
                         :kaocha/tests
                         [{:kaocha.testable/type      :kaocha.type/suite
                           :kaocha.testable/id        :aaa
                           :kaocha.suite/ns-patterns  ["^foo$"]
                           :kaocha.suite/source-paths ["src"]
                           :kaocha.suite/test-paths   ["fixtures/a-tests"]}]}
                 :err  ""}
                (-> (invoke-with-config {:tests [{:id          :aaa
                                                  :test-paths  ["fixtures/a-tests"]
                                                  :ns-patterns ["^foo$"]}]}
                                        "--print-config")

                    (update :out read-string)))))

  (testing "it elegantly reports when no tests are found"
    (is (match? (invoke-with-config {:color? false
                                     :tests  [{:id          :empty
                                               :test-paths  ["fixtures/a-tests"]
                                               :ns-patterns ["^foo$"]}]})
                {:exit 0, :out "\n0 test vars, 0 assertions, 0 failures.\n", :err ""})))

  (testing "--fail-fast"
    (is (match? {:err  ""
                 :out  (str ".\n..F\n\n"
                            "FAIL in (fail-1) (hello_test.clj:12)\n"
                            "expected: false\n"
                            "  actual: false\n"
                            "3 test vars, 4 assertions, 1 failures.\n")
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
                (invoke-runner "--reporter" "does/not-exist"))))

  (testing "Exception outside `is` with fail-fast"
    (is (match? {:exit 1,
                 :out
                 "E

ERROR in (exception-outside-is-test) (exception_outside_is.clj:4)
Uncaught exception, not in assertion.
Exception: java.lang.Exception: booo
 at ddd.exception_outside_is$fn__12380.invokeStatic (exception_outside_is.clj:4)
    ddd.exception_outside_is/fn (exception_outside_is.clj:4)
    kaocha.type.var$eval12416$fn__12418$fn__12420$fn__12421.invoke (var.clj:14)
    kaocha.type.var$eval12416$fn__12418$fn__12420.invoke (var.clj:13)
    kaocha.type.var$eval12416$fn__12418.invoke (var.clj:11)
    ...
    kaocha.testable$run.invokeStatic (testable.clj:86)
    kaocha.testable$run.invoke (testable.clj:79)
    kaocha.testable$run_testables.invokeStatic (testable.clj:107)
    kaocha.testable$run_testables.invoke (testable.clj:97)
    kaocha.type.ns$eval12013$fn__12014.invoke (ns.clj:56)
    ...
    kaocha.testable$run.invokeStatic (testable.clj:86)
    kaocha.testable$run.invoke (testable.clj:79)
    kaocha.testable$run_testables.invokeStatic (testable.clj:107)
    kaocha.testable$run_testables.invoke (testable.clj:97)
    kaocha.type.suite$eval12325$fn__12326.invoke (suite.clj:34)
    ...
    kaocha.testable$run.invokeStatic (testable.clj:86)
    kaocha.testable$run.invoke (testable.clj:79)
    kaocha.testable$run_testables.invokeStatic (testable.clj:107)
    kaocha.testable$run_testables.invoke (testable.clj:97)
    kaocha.api$run$fn__2484.invoke (api.clj:69)
    ...
    kaocha.api$run.invokeStatic (api.clj:59)
    kaocha.api$run.invoke (api.clj:45)
    kaocha.runner$_main_STAR_.invokeStatic (runner.clj:106)
    kaocha.runner$_main_STAR_.doInvoke (runner.clj:53)
    ...
    kaocha.runner$_main.invokeStatic (runner.clj:115)
    kaocha.runner$_main.doInvoke (runner.clj:113)
    ...
    clojure.main$main_opt.invokeStatic (main.clj:317)
    clojure.main$main_opt.invoke (main.clj:313)
    clojure.main$main.invokeStatic (main.clj:424)
    clojure.main$main.doInvoke (main.clj:387)
    ...
    clojure.main.main (main.java:37)
1 test vars, 1 assertions, 1 errors, 0 failures.
",
                 :err ""}

                (invoke-runner "--config-file" "fixtures/with_exception.edn" "outside-is" "--fail-fast")))))

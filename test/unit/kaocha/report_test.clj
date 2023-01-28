(ns kaocha.report-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.report :as report]
            [kaocha.type :as type]
            [kaocha.test-util :refer [with-test-out-str]]
            [kaocha.output :as output]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.history :as history]
            [kaocha.testable :as testable]
            [slingshot.slingshot :refer [try+]]))

(require 'kaocha.assertions)

(deftest dispatch-extra-keys-test
  (testing "it dispatches to custom clojure.test/report extensions"
    (.addMethod report/clojure-test-report
                ::yolo
                (fn [m]
                  (clojure.test/with-test-out
                    (println "YOLO expected"
                             (:expected m)
                             "actual"
                             (:actual m)))))

    (is (= "YOLO expected :x actual :y\n"
           (with-test-out-str
             (report/dispatch-extra-keys {:type ::yolo
                                     :expected :x
                                     :actual :y})))))

  (testing "it does nothing if there is no matching multimethod implementation"
    (is (= ""
           (with-test-out-str
             (report/dispatch-extra-keys {:type ::nolo})))))

  (testing "it does nothing if it's a key known to Kaocha"
    (hierarchy/derive! ::knowlo :kaocha/known-key)
    (.addMethod report/clojure-test-report
                ::knowlo
                (fn [m] (clojure.test/with-test-out (println "KNOWLO"))))
    (is (= ""
           (with-test-out-str
             (report/dispatch-extra-keys {:type ::knowlo})))))

  (testing "it does nothing if the key is globally marked as \"known\""
    (derive ::knowlo :kaocha/known-key)
    (.addMethod report/clojure-test-report
                ::knowlo
                (fn [m] (clojure.test/with-test-out (println "KNOWLO"))))
    (is (= ""
           (with-test-out-str
             (report/dispatch-extra-keys {:type ::knowlo}))))))

(deftest dots*-test
  (is (= "."
         (with-test-out-str
           (report/dots* {:type :pass}))))

  (is (= "[31mF[m"
         (with-test-out-str
           (report/dots* {:type :fail}))))

  (is (= "[31mE[m"
         (with-test-out-str
           (report/dots* {:type :error}))))

  (is (= "[33mP[m"
         (with-test-out-str
           (report/dots* {:type :kaocha/pending}))))

  (is (= "("
         (with-test-out-str
           (report/dots* {:type :kaocha/begin-group}))))

  (is (= ")"
         (with-test-out-str
           (report/dots* {:type :kaocha/end-group}))))

  (is (= "["
         (with-test-out-str
           (report/dots* {:type :begin-test-suite}))))

  (is (= "]"
         (with-test-out-str
           (report/dots* {:type :end-test-suite}))))

  (is (= "\n"
         (with-test-out-str
           (report/dots* {:type :summary})))))

(deftest report-counters-test
  (is (= #:kaocha.result {:pass 1 :error 0 :fail 0 :pending 0}
         (type/with-report-counters
           (report/report-counters {:type :pass})
           (type/report-count))))

  (is (= #:kaocha.result {:pass 0 :error 0 :fail 1 :pending 0}
         (type/with-report-counters
           (report/report-counters {:type :fail})
           (type/report-count))))

  (is (= #:kaocha.result {:pass 0 :error 1 :fail 0 :pending 0}
         (type/with-report-counters
           (report/report-counters {:type :error})
           (type/report-count))))

  (is (= #:kaocha.result {:pass 0 :error 0 :fail 0 :pending 1}
         (type/with-report-counters
           (report/report-counters {:type :kaocha/pending})
           (type/report-count)))))

(deftest testing-vars-str-test
  (testing "getting info from testable"
    (is (= "foo/bar (foo.clj:33)"
           (report/testing-vars-str {:kaocha/testable
                                {:kaocha.testable/meta
                                 {:file "foo.clj"
                                  :line 33}
                                 :kaocha.testable/id :foo/bar}}))))

  (testing "explicit file/line override"
    (is (= "foo/bar (foo.clj:33)"
           (report/testing-vars-str {:file "foo.clj"
                                :line 33
                                :kaocha/testable {:kaocha.testable/id :foo/bar
                                                  :file "bar.clj"
                                                  :line 44}}))))

  (testing "clojure.test legacy compatiblity"
    (is (= "(report-counters-test) (foo.clj:33)"
           (report/testing-vars-str {:file "foo.clj"
                                :line 33
                                :testing-vars [#'report-counters-test]})))))

(deftest print-output-test
  (is (= (str "╭───── Test output ───────────────────────────────────────────────────────\n"
              "│ foo\n"
              "╰─────────────────────────────────────────────────────────────────────────\n")
         (with-out-str
           (report/print-output {:kaocha/testable {:kaocha.plugin.capture-output/output "foo"}})))))

(deftest assertion-type-test
  (is (= '=
         (report/assertion-type {:expected '(= 1 2)})))

  (is (= :default
         (report/assertion-type {}))))

(deftest print-expr-test
  (is (= "expected: 1\n  actual: 2\n"
         (with-out-str
           (report/print-expr {:expected 1
                          :actual 2}))))

  (is (= "Expected:\n  [36m1[0m\nActual:\n  [31m-1[0m [32m+2[0m\n"
         (with-out-str
           (report/print-expr {:expected '(= 1 (+ 1 1))
                          :actual '(not (= 1 2))})))))

(deftest fail-summary-test
  (is (= (str   "\n"
                "[31mFAIL[m in foo/bar-test (foo.clj:42)\n"
                "it does the thing\n"
                "Numbers are not equal\n"
                "Expected:\n"
                "  [36m1[0m\n"
                "Actual:\n"
                "  [31m-1[0m [32m+2[0m\n")
         (with-out-str
           (report/fail-summary {:type :fail
                            :file "foo.clj"
                            :line 42
                            :kaocha/testable {:kaocha.testable/id :foo/bar-test}
                            :testing-contexts ["it does the thing"]
                            :expected '(= 1 (+ 1 1))
                            :actual '(not (= 1 2))
                            :message "Numbers are not equal"}))))

  (is (= (str "\n"
              "[31mFAIL[m in foo/bar-test (foo.clj:42)\n"
              "Numbers are not equal\n"
              "Oh no!")
         (with-out-str
           (report/fail-summary {:type :fail
                            :file "foo.clj"
                            :line 42
                            :kaocha/testable {:kaocha.testable/id :foo/bar-test}
                            :expected '(= 1 (+ 1 1))
                            :actual '(not (= 1 2))
                            :kaocha.report/printed-expression "Oh no!"
                            :message "Numbers are not equal"}))))

  (is (substring? (str "[31mERROR[m in foo/bar-test (foo.clj:42)\n"
                       "Numbers are not equal\n"
                       "Exception: clojure.lang.ExceptionInfo: a message\n")
                  (with-out-str
                    (report/fail-summary {:type :error
                                     :file "foo.clj"
                                     :line 42
                                     :kaocha/testable {:kaocha.testable/id :foo/bar-test}
                                     :actual (ex-info "a message" {:some :info})
                                     :message "Numbers are not equal"}))))

  (is (= (str "\n"
              "[31mERROR[m in foo/bar-test (foo.clj:42)\n"
              "Numbers are not equal\n"
              "Oh no!")
         (with-out-str
           (report/fail-summary {:type :error
                            :file "foo.clj"
                            :line 42
                            :kaocha/testable {:kaocha.testable/id :foo/bar-test}
                            :kaocha.report/printed-expression "Oh no!"
                            :message "Numbers are not equal"})))))

(deftest result-test
  (is (= "[31m5 tests, 9 assertions, 2 errors, 1 pending, 3 failures.[m\n"
         (with-test-out-str
           (binding [history/*history* (atom [])]
             (report/result {:type :summary :test 5 :pass 4 :fail 3 :error 2 :pending 1})))))

  (is (= "[32m5 tests, 5 assertions, 0 failures.[m\n"
         (with-test-out-str
           (binding [history/*history* (atom [])]
             (report/result {:type :summary :test 5 :pass 5 :fail 0 :error 0 :pending 0}))))))

(deftest result-failures-test
  (is (= "\n[31mFAIL[m in foo/bar-test (foo.clj:42)\nit does the thing\nNumbers are not equal\nExpected:\n  [36m1[0m\nActual:\n  [31m-1[0m [32m+2[0m\n[31m5 tests, 6 assertions, 1 failures.[m\n"
         (with-test-out-str
           (binding [
                     history/*history* (atom [{:type :fail
                                               :file "foo.clj"
                                               :line 42
                                               :kaocha/testable {:kaocha.testable/id :foo/bar-test}
                                               :testing-contexts ["it does the thing"]
                                               :expected '(= 1 (+ 1 1))
                                               :actual '(not (= 1 2))
                                               :message "Numbers are not equal"}])]
             (report/result {:type :summary :test 5 :pass 5 :fail 1 :error 0 :pending 0}))))))

(deftest fail-fast-test
  (is (nil? (binding [testable/*fail-fast?* false]
              (report/fail-fast {:type :fail}))))
  (is (= :caught (try
                   (try+
                    (binding [testable/*fail-fast?* true]
                      (report/fail-fast {:type :fail}))
                    (catch :kaocha/fail-fast e
                      :caught))
                   (catch Throwable e
                     :not-caught)))))

(deftest doc-print-contexts-test
  (is (= "\n    Level 1\n      Level 2A\n      Level 2B"
         (with-redefs [report/doc-printed-contexts (atom nil)]
           (with-out-str
             (report/doc-print-contexts ["Level 2A" "Level 1"])
             (report/doc-print-contexts ["Level 2B" "Level 1"]))))))

(deftest doc-test
  (is (= "--- xxx ---------------------------"
         (with-test-out-str
           (report/doc {:type :begin-test-suite
                   :kaocha/testable {:kaocha.testable/desc "xxx"}}))))


  (is (= "\nxxx"
         (with-test-out-str
           (report/doc {:type :begin-test-ns
                   :kaocha/testable {:kaocha.testable/desc "xxx"}}))))

  (is (= "\n"
         (with-test-out-str
           (report/doc {:type :end-test-ns
                   :kaocha/testable {:kaocha.testable/desc "xxx"}}))))

  (is (= "\n  xxx"
         (with-test-out-str
           (report/doc {:type :begin-test-var
                   :kaocha/testable {:kaocha.testable/desc "xxx"}}))))

  (is (= "\n    level1\n      level2"
         (with-test-out-str
           (with-redefs [report/doc-printed-contexts (atom nil)]
             (binding [t/*testing-contexts* ["level2" "level1"]]
               (report/doc {:type :pass}))))))

  (is (= "\n    level1\n      level2[31m ERROR[m"
         (with-test-out-str
           (with-redefs [report/doc-printed-contexts (atom nil)]
             (binding [t/*testing-contexts* ["level2" "level1"]]
               (report/doc {:type :error}))))))

  (is (= "\n    level1\n      level2[31m FAIL[m"
         (with-test-out-str
           (with-redefs [report/doc-printed-contexts (atom nil)]
             (binding [t/*testing-contexts* ["level2" "level1"]]
               (report/doc {:type :fail}))))))

  (is (= "\n"
         (with-test-out-str
           (report/doc {:type :summary})))))

(deftest tap-test
  (is (= "ok  (foo.clj:20)\n"
         (with-test-out-str
           (binding [output/*colored-output* false]
             (report/tap {:type :pass
                     :file "foo.clj"
                     :line 20})))))

  (is (= (str "not ok  (foo.clj:20)\n"
              "#  FAIL in  (foo.clj:20)\n"
              "#  Expected:\n"
              "#    3\n"
              "#  Actual:\n"
              "#    -3 +4\n")
         (with-test-out-str
           (binding [output/*colored-output* false]
             (report/tap {:type :fail
                     :file "foo.clj"
                     :line 20
                     :expected '(= 3 4)
                     :actual '(not (= 3 4))}))))))

(comment
  (do
    (require 'kaocha.repl)
    (kaocha.repl/run)))

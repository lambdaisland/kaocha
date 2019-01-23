(ns kaocha.type.var-test
  (:refer-clojure :exclude [symbol])
  (:require [clojure.test :as t :refer :all]
            [kaocha.test-factories :as f]
            [kaocha.testable :as testable]
            [kaocha.report :as report]
            [kaocha.classpath]
            [kaocha.test-helper]
            [kaocha.core-ext :refer :all]
            [kaocha.config :as config]
            [kaocha.test-util :refer [with-test-ctx]]
            [kaocha.type.var]))

(deftest run-test
  (testing "a passing test var"
    (kaocha.classpath/add-classpath "fixtures/a-tests")
    (require 'foo.bar-test)
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? true}
            (testable/run {:kaocha.testable/type :kaocha.type/var
                           :kaocha.testable/id   :foo.bar-test/a-test
                           :kaocha.testable/desc "a-test"
                           :kaocha.var/name      'foo.bar-test/a-test
                           :kaocha.var/var       (resolve 'foo.bar-test/a-test)
                           :kaocha.var/test      (-> (resolve 'foo.bar-test/a-test) meta :test)}
              (f/test-plan {})))]

      (is (match? {:kaocha.testable/type :kaocha.type/var
                   :kaocha.testable/id   :foo.bar-test/a-test
                   :kaocha.testable/desc "a-test"
                   :kaocha.var/name      'foo.bar-test/a-test
                   :kaocha.var/var       (resolve 'foo.bar-test/a-test)
                   :kaocha.var/test      fn?
                   :kaocha.result/count  1
                   :kaocha.result/pass   1
                   :kaocha.result/error  0
                   :kaocha.result/fail   0}
                  result))

      (is (match? [{:type :begin-test-var, :var (resolve 'foo.bar-test/a-test)}
                   {:type :pass, :expected true, :actual true, :message nil}
                   {:type :end-test-var, :var (resolve 'foo.bar-test/a-test)}]
                  report))))

  (testing "a failing test var"
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? true}
            (testable/run
              (f/var-testable {:kaocha.var/test (fn [] (is false))})
              (f/test-plan {})))]

      (is (match? {:kaocha.result/count  1
                   :kaocha.result/pass   0
                   :kaocha.result/error  0
                   :kaocha.result/fail   1}
                  result))

      (is (match? [{:type :begin-test-var, :var var?}
                   {:type :fail, :expected false, :actual false, :message nil}
                   {:type :end-test-var, :var var?}]
                  report))))

  (testing "an erroring test var"
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? true}
            (testable/run
              (f/var-testable {:kaocha.var/test (fn [] (throw (ex-info "ERROR!" {})))})
              (f/test-plan {})))]

      (is (match? {:kaocha.result/count  1
                   :kaocha.result/pass   0
                   :kaocha.result/error  1
                   :kaocha.result/fail   0}
                  result))

      (is (match? [{:type :begin-test-var, :var var?}
                   {:type :error
                    ;; TODO on CI these are nil. A var without metadata?
                    ;; :file string?
                    ;; :line pos-int?
                    :expected nil
                    :actual exception?
                    :message "Uncaught exception, not in assertion."}
                   {:type :end-test-var, :var var?}]
                  report))))

  (testing "multiple assertions"
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? false}
            (testable/run
              (f/var-testable {:kaocha.var/test (fn []
                                                  (is true)
                                                  (is true)
                                                  (is false)
                                                  (is true))})
              (f/test-plan {})))]

      (is (match? {:kaocha.result/count  1
                   :kaocha.result/pass   3
                   :kaocha.result/error  0
                   :kaocha.result/fail   1}
                  result))

      (is (match? [{:type :begin-test-var, :var var?}
                   {:type :pass}
                   {:type :pass}
                   {:type :fail,
                    ;;:file string?,
                    ;;:line pos-int?
                    }
                   {:type :pass}
                   {:type :end-test-var, :var var?}]
                  report))))

  (testing "early exit"
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? true}
            (testable/run
              (f/var-testable {:kaocha.var/test (fn []
                                                  (is true)
                                                  (is true)
                                                  (is false)
                                                  (is true))})
              (f/test-plan {})))]

      (is (match? {:kaocha.result/count  1
                   :kaocha.result/pass   2
                   :kaocha.result/error  0
                   :kaocha.result/fail   1}
                  result))

      (is (match? [{:type :begin-test-var, :var var?}
                   {:type :pass}
                   {:type :pass}
                   {:type :fail
                    ;;:file string?,
                    ;;:line pos-int?
                    }
                   {:type :end-test-var, :var var?}]
                  report)))

    (testing "early exit - exception"
      (let [{:keys [result report]}
            (with-test-ctx {:fail-fast? true}
              (testable/run
                (f/var-testable {:kaocha.var/test (fn []
                                                    (is true)
                                                    (is true)
                                                    (throw (Exception. "ERROR!"))
                                                    (is true))})
                (f/test-plan {})))]

        (is (match? {:kaocha.result/count  1
                     :kaocha.result/pass   2
                     :kaocha.result/error  1
                     :kaocha.result/fail   0}
                    result))

        (is (match? [{:type :begin-test-var, :var var?}
                     {:type :pass}
                     {:type :pass}
                     {:type :error
                      ;;:file string?
                      ;;:line pos-int?
                      }
                     {:type :end-test-var, :var var?}]
                    report))))))

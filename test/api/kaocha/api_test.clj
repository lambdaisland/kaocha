(ns kaocha.api-test
  (:require [clojure.test :as t :refer :all]
            [kaocha.test-helper :refer :all]
            [kaocha.core-ext :refer :all]
            [kaocha.api :as api]
            [kaocha.specs]
            [clojure.spec.alpha :as s]
            [kaocha.report :as report]
            [kaocha.test-factories :as f])
  (:import [clojure.lang Var]))

(def ^:dynamic *report-history* nil)

(defmacro with-test-ctx
  "When testing lower level functions, make sure the necessary shared state is set up."
  [opts & body]
  `(binding [t/*report-counters* (ref t/*initial-report-counters*)
             t/*testing-vars* (list)
             *report-history* (atom [])]
     (with-redefs [t/report (fn [m#]
                              (swap! *report-history* conj m#)
                              (report/report-counters m#)
                              (when (:fail-fast? ~opts) (report/fail-fast m#)))]
       (let [result# (do ~@body)]
         {:result result#
          :report @*report-history*}))))

(defn var-name?
  "Predicate for the name of a var, for use in matchers."
  [v n]
  (and (var? v) (= (:name (meta v)) n)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def :kaocha.type/unknown map?)

(deftest load-testable--default
  (is (thrown-ex-data?  "No implementation of kaocha.api/load-testable for :kaocha.type/unknown"
                        {:kaocha.error/reason         :kaocha.error/missing-method,
                         :kaocha.error/missing-method 'kaocha.api/load-testable,
                         :kaocha/testable             {:kaocha.testable/type :kaocha.type/unknown
                                                       :kaocha.testable/id   :foo}}
                        (api/load-testable {:kaocha.testable/type :kaocha.type/unknown
                                            :kaocha.testable/id   :foo}))))

(deftest load-testable--ns
  (let [ns-name (doto (gensym "test.ns")
                  create-ns
                  (intern (with-meta 'test-1 {:test :test-1}) nil)
                  (intern (with-meta 'test-2 {:test :test-2}) nil))
        testable (api/load-testable {:kaocha.testable/type :kaocha.type/ns
                                     :kaocha.testable/id   (keyword ns-name)
                                     :kaocha.ns/name       ns-name})]

    (is (match? {:kaocha.testable/type :kaocha.type/ns
                 :kaocha.ns/name       ns-name
                 :kaocha.test-plan/tests [{:kaocha.testable/type :kaocha.type/var,
                                           :kaocha.testable/id   (keyword (str ns-name) "test-1")
                                           :kaocha.var/name      (symbol (str ns-name) "test-1")
                                           :kaocha.var/var       #(var-name? % 'test-1)
                                           :kaocha.var/test      :test-1}
                                          {:kaocha.testable/type :kaocha.type/var
                                           :kaocha.testable/id   (keyword (str ns-name) "test-2")
                                           :kaocha.var/name      (symbol (str ns-name) "test-2")
                                           :kaocha.var/var       #(var-name? % 'test-2)
                                           :kaocha.var/test      :test-2}]}
                testable))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest run-testable--default
  (is (thrown-ex-data?  "No implementation of kaocha.api/run-testable for :kaocha.type/unknown"
                        {:kaocha.error/reason         :kaocha.error/missing-method,
                         :kaocha.error/missing-method 'kaocha.api/run-testable,
                         :kaocha/testable             #:kaocha.testable{:type :kaocha.type/unknown
                                                                        :id   :foo}}
                        (api/run-testable #:kaocha.testable{:type :kaocha.type/unknown
                                                            :id   :foo}))))




(deftest run-testable--var
  (testing "a passing test var"
    (kaocha.classpath/add-classpath "fixtures/a-tests")
    (require 'foo.bar-test)
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? true}
            (api/run-testable {:kaocha.testable/type :kaocha.type/var
                               :kaocha.testable/id   :foo.bar-test/a-test,
                               :kaocha.var/name      'foo.bar-test/a-test
                               :kaocha.var/var       (resolve 'foo.bar-test/a-test)
                               :kaocha.var/test      (-> (resolve 'foo.bar-test/a-test) meta :test)}))]

      (is (match? {:kaocha.testable/type :kaocha.type/var
                   :kaocha.testable/id   :foo.bar-test/a-test
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
            (api/run-testable
             (f/var-testable {:kaocha.var/test (fn [] (is false))})))]

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
            (api/run-testable
             (f/var-testable {:kaocha.var/test (fn [] (throw (ex-info "ERROR!" {})))})))]

      (is (match? {:kaocha.result/count  1
                   :kaocha.result/pass   0
                   :kaocha.result/error  1
                   :kaocha.result/fail   0}
                  result))

      (is (match? [{:type :begin-test-var, :var var?}
                   {:type :error
                    :file string?
                    :line pos-int?
                    :expected nil
                    :actual exception?
                    :message "Uncaught exception, not in assertion."}
                   {:type :end-test-var, :var var?}]
                  report))))

  (testing "multiple assertions"
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? false}
            (api/run-testable
             (f/var-testable {:kaocha.var/test (fn []
                                                 (is true)
                                                 (is true)
                                                 (is false)
                                                 (is true))})))]

      (is (match? {:kaocha.result/count  1
                   :kaocha.result/pass   3
                   :kaocha.result/error  0
                   :kaocha.result/fail   1}
                  result))

      (is (match? [{:type :begin-test-var, :var var?}
                   {:type :pass}
                   {:type :pass}
                   {:type :fail, :file string?, :line pos-int?}
                   {:type :pass}
                   {:type :end-test-var, :var var?}]
                  report))))

  (testing "early exit"
    (let [{:keys [result report]}
          (with-test-ctx {:fail-fast? true}
            (api/run-testable
             (f/var-testable {:kaocha.var/test (fn []
                                                 (is true)
                                                 (is true)
                                                 (is false)
                                                 (is true))})))]

      (is (match? {:kaocha.result/count  1
                   :kaocha.result/pass   2
                   :kaocha.result/error  0
                   :kaocha.result/fail   1}
                  result))

      (is (match? [{:type :begin-test-var, :var var?}
                   {:type :pass}
                   {:type :pass}
                   {:type :fail, :file string?, :line pos-int?}
                   {:type :end-test-var, :var var?}]
                  report)))

    (testing "early exit - exception"
      (let [{:keys [result report]}
            (with-test-ctx {:fail-fast? true}
              (api/run-testable
               (f/var-testable {:kaocha.var/test (fn []
                                                   (is true)
                                                   (is true)
                                                   (throw (Exception. "ERROR!"))
                                                   (is true))})))]

        (is (match? {:kaocha.result/count  1
                     :kaocha.result/pass   2
                     :kaocha.result/error  1
                     :kaocha.result/fail   0}
                    result))

        (is (match? [{:type :begin-test-var, :var var?}
                     {:type :pass}
                     {:type :pass}
                     {:type :error, :file string?, :line pos-int?}
                     {:type :end-test-var, :var var?}]
                    report))))))

#_
(run-tests)

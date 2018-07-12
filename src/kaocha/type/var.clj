(ns kaocha.type.var
  (:require [clojure.test :as t]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:import [clojure.lang Var]))

(defmethod testable/-run :kaocha.type/var [{:kaocha.var/keys [var test] :as testable}]
  (let [initial-report @t/*report-counters*]
    (binding [t/*testing-vars* (conj t/*testing-vars* var)]
      (t/do-report {:type :begin-test-var, :var var})
      (try
        (test)
        (catch clojure.lang.ExceptionInfo e
          (when-not (:kaocha/fail-fast (ex-data e))
            (t/do-report {:type :error
                          :message "Uncaught exception, not in assertion."
                          :expected nil
                          :actual e
                          :kaocha.result/exception e
                          :kaocha/testable testable})))
        (catch Throwable e
          (t/do-report {:type :error
                        :message "Uncaught exception, not in assertion."
                        :expected nil
                        :actual e
                        :kaocha.result/exception e
                        :kaocha/testable testable})))
      (t/do-report {:type :end-test-var, :var var}))
    (merge testable
           {:kaocha.result/count 1}
           (result/diff-test-result initial-report @t/*report-counters*))))

(s/def :kaocha.type/var (s/keys :req [:kaocha.testable/type
                                      :kaocha.testable/id
                                      :kaocha.var/name
                                      :kaocha.var/var
                                      :kaocha.var/test]))

(s/def :kaocha.var/name qualified-symbol?)
(s/def :kaocha.var/test (s/spec ifn?
                                :gen (fn []
                                       (gen/one-of [(gen/return (fn [] (t/is true)))
                                                    (gen/return (fn [] (t/is false)))]))))
(s/def :kaocha.var/var (s/spec var?
                               :gen (fn []
                                      (gen/return (.setDynamic (Var/create))))))

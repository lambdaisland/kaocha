(ns kaocha.type.var
  (:require [clojure.test :as t]
            [kaocha.type :as type]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [kaocha.report :as report]
            [kaocha.hierarchy :as hierarchy]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str])
  (:import [clojure.lang Var]))

(defmethod report/fail-summary ::zero-assertions [{:keys [testing-contexts testing-vars] :as m}]
  (println "\nFAIL in" (report/testing-vars-str m))
  (when (seq testing-contexts)
    (println (str/join " " testing-contexts)))
  (println "Test ran without assertions. Did you forget an (is ...)?")
  (report/print-output m))

(defn wrap-test [test the-var]
  (binding [t/*testing-vars* (conj t/*testing-vars* the-var)]
    (t/do-report {:type :begin-test-var, :var the-var})
    (try
      (test)
      (catch clojure.lang.ExceptionInfo e
        (when-not (:kaocha/fail-fast (ex-data e))
          (report/report-exception e)))
      (catch Throwable e (report/report-exception e)))))

(defmethod testable/-run :kaocha.type/var [{test    :kaocha.var/test
                                            wrap    :kaocha.testable/wrap
                                            the-var :kaocha.var/var
                                            meta'   :kaocha.testable/meta
                                            :as     testable} test-plan]
  (type/with-report-counters
    (let [test-fn (fn [] (wrap-test test the-var))
          merged-wraps (reduce #(%2 %1) test-fn wrap)]
      (merged-wraps)
      (let [{::result/keys [pass error fail pending] :as result} (type/report-count)]
        (when (= pass error fail pending 0)
          (binding [testable/*fail-fast?* false
                    testable/*test-location* {:file (:file meta') :line (:line meta')}]
            (t/do-report {:type ::zero-assertions})))
        (t/do-report {:type :end-test-var, :var the-var})
        (merge testable {:kaocha.result/count 1} (type/report-count))))))

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

(hierarchy/derive! :kaocha/begin-var :kaocha/begin-test)
(hierarchy/derive! :kaocha/end-var :kaocha/end-test)

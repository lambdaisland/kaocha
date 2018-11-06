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

(hierarchy/derive! ::zero-assertions :kaocha/known-key)
(hierarchy/derive! ::zero-assertions :kaocha/fail-type)

(defmethod report/fail-summary ::zero-assertions [{:keys [testing-contexts testing-vars] :as m}]
  (println "\nFAIL in" (report/testing-vars-str m))
  (when (seq testing-contexts)
    (println (str/join " " testing-contexts)))
  (println "Test ran without assertions. Did you forget an (is ...)?"))

(defmethod testable/-run :kaocha.type/var [{:kaocha.var/keys [test wrap]
                                            the-var :kaocha.var/var
                                            :as testable} test-plan]
  (type/with-report-counters
    (let [test (reduce #(%2 %1) test wrap)]
      (binding [t/*testing-vars* (conj t/*testing-vars* the-var)]
        (t/do-report {:type :begin-test-var, :var the-var})
        (try
          (test)
          (catch clojure.lang.ExceptionInfo e
            (when-not (:kaocha/fail-fast (ex-data e))
              (t/do-report {:type :error
                            :message "Uncaught exception, not in assertion."
                            :expected nil
                            :actual e
                            :kaocha.result/exception e})))
          (catch Throwable e
            (t/do-report {:type :error
                          :message "Uncaught exception, not in assertion."
                          :expected nil
                          :actual e
                          :kaocha.result/exception e}))))
      (let [{::result/keys [pass error fail pending] :as result} (type/report-count)]
        (when (= pass error fail pending 0)
          (binding [testable/*fail-fast?* false]
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

(derive :kaocha.type/var :kaocha.testable.type/leaf)

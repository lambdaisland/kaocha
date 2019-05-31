(ns kaocha.ns
  (:refer-clojure :exclude [symbol])
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :as t]
            [kaocha.core-ext :refer :all]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [kaocha.type :as type]))

(defn run-tests [testable test-plan fixture-fn]
  ;; It's not guaranteed the the fixture-fn returns the result of calling the
  ;; tests function, so we need to put it in a box for reference.
  (let [result (atom (:kaocha.test-plan/tests testable))]
    (fixture-fn #(swap! result testable/run-testables test-plan))
    @result))

(defn run-testable [testable test-plan]
  (let [do-report #(t/do-report (merge {:ns (:kaocha.ns/ns testable)} %))]
    (type/with-report-counters
      (do-report {:type :begin-test-ns})
      (if-let [load-error (:kaocha.test-plan/load-error testable)]
        (do
          (do-report {:type                    :error
                      :message                 "Failed to load namespace."
                      :expected                nil
                      :actual                  load-error
                      :kaocha.result/exception load-error})
          (do-report {:type :end-test-ns})
          (assoc testable :kaocha.result/error 1))
        (let [ns-meta         (:kaocha.testable/meta testable)
              once-fixture-fn (t/join-fixtures (::t/once-fixtures ns-meta))
              tests           (run-tests testable test-plan once-fixture-fn)
              result          (assoc (dissoc testable :kaocha.test-plan/tests)
                                     :kaocha.result/tests
                                     tests)]
          (do-report {:type :end-test-ns})
          result)))))

(defn required-ns [ns-name]
  (when-not (find-ns ns-name)
    (require ns-name))
  ns-name)

(defn starts-with-namespace? [ns-name sym-or-kw]
  (-> sym-or-kw namespace (= (str ns-name))))

(s/def :kaocha.ns/name simple-symbol?)
(s/def :kaocha.ns/ns   ns?)

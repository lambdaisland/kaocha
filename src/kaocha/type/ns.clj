(ns kaocha.type.ns
  (:refer-clojure :exclude [symbol])
  (:require [clojure.spec.alpha :as spec]
            [clojure.test :as t]
            [kaocha.core-ext :refer :all]
            [kaocha.ns :as ns]
            [kaocha.testable :as testable]
            [kaocha.type :as type]))

(defn ->testable [ns-name]
  {:kaocha.testable/type            :kaocha.type/ns
   :kaocha.testable/id              (keyword (str ns-name))
   :kaocha.testable/desc            (str ns-name)
   :kaocha.testable/parallelizable? true
   :kaocha.ns/name                  ns-name})

(defn run-tests [testable test-plan fixture-fn]
  ;; It's not guaranteed the the fixture-fn returns the result of calling the
  ;; tests function, so we need to put it in a box for reference.
  (let [result (promise)]
    (fixture-fn
     (fn []
       (deliver result (testable/run-testables-parent testable test-plan))))
    @result))

(defmethod testable/-load :kaocha.type/ns [testable]
  ;; TODO If the namespace has a test-ns-hook function, call that:
  ;; if-let [v (find-var (symbol (:kaocha.ns/name testable) "test-ns-hook"))]

  (let [ns-name         (:kaocha.ns/name testable)
        ns-obj          (ns/required-ns ns-name)
        ns-meta         (meta ns-obj)
        each-fixture-fn (t/join-fixtures (::t/each-fixtures ns-meta))]
    (assoc testable
           :kaocha.testable/meta (meta ns-obj)
           :kaocha.ns/ns ns-obj
           :kaocha.test-plan/tests
           (->> ns-obj
                ns-interns
                (filter (comp :test meta val))
                (sort-by key)
                (map (fn [[sym var]]
                       (let [nsname    (:kaocha.ns/name testable)
                             test-name (symbol (str nsname) (str sym))]
                         {:kaocha.testable/type :kaocha.type/var
                          :kaocha.testable/id   (keyword test-name)
                          :kaocha.testable/meta (meta var)
                          :kaocha.testable/desc (str sym)
                          :kaocha.var/name      test-name
                          :kaocha.var/var       var
                          :kaocha.var/test      (:test (meta var))
                          :kaocha.testable/wrap (if (::t/each-fixtures ns-meta)
                                                  [(fn [t] #(each-fixture-fn t))]
                                                  [])})))))))

(defmethod testable/-run :kaocha.type/ns [testable test-plan]
  (let [do-report #(t/do-report (merge {:ns (:kaocha.ns/ns testable)} %))]
    (type/with-report-counters
      (do-report {:type :begin-test-ns})
      (let [ns-meta         (:kaocha.testable/meta testable)
            once-fixture-fn (t/join-fixtures (::t/once-fixtures ns-meta))
            tests           (run-tests testable test-plan once-fixture-fn)
            result          (assoc (dissoc testable :kaocha.test-plan/tests)
                                   :kaocha.result/tests
                                   tests)]
        (do-report {:type :end-test-ns})
        result))))

(spec/def :kaocha.type/ns (spec/keys :req [:kaocha.testable/type
                                           :kaocha.testable/id
                                           :kaocha.ns/name]
                                     :opt [:kaocha.ns/ns
                                           :kaocha.test-plan/tests]))

(ns kaocha.api
  "Programmable test runner interface. Contains both the API to call Kaocha to run
  your tests, and the multimethod extension points to extend Kaocha to other
  test frameworks."
  (:require [clojure.test :as t]
            [clojure.spec.alpha :as s]
            [kaocha.specs :refer [assert-spec]]
            [kaocha.monkey-patch]
            [slingshot.slingshot :refer [try+ throw+]]))

(defn- testable-type [testable]
  (assert-spec :kaocha/testable testable)
  (let [type (:kaocha.testable/type testable)]
    (assert-spec type testable)
    type))

(defmulti load-testable testable-type)

(defmethod load-testable :default [testable]
  (throw (ex-info (str "No implementation of "
                       `load-testable
                       " for "
                       (pr-str (:kaocha.testable/type testable)))
                  {:kaocha.error/reason         :kaocha.error/missing-method
                   :kaocha.error/missing-method `load-testable
                   :kaocha/testable             testable})))

(defmethod load-testable :kaocha.type/ns [testable]
  (->> testable
       :kaocha.ns/name
       the-ns
       ns-publics
       (filter (comp :test meta val))
       (map (fn [[sym var]]
              (let [nsname    (:kaocha.ns/name testable)
                    test-name (symbol (str nsname) (str sym))]
                {:kaocha.testable/type :kaocha.type/var
                 :kaocha.testable/id   (keyword test-name)
                 :kaocha.var/name      test-name
                 :kaocha.var/var       var
                 :kaocha.var/test      (:test (meta var))})))
       (assoc testable :kaocha.test-plan/tests)))

(s/fdef load-testable
        :args (s/cat :testable :kaocha/testable)
        :ret :kaocha.test-plan/testable)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti run-testable testable-type)

(defmethod run-testable :default [testable]
  (throw (ex-info (str "No implementation of "
                       `run-testable
                       " for "
                       (pr-str (:kaocha.testable/type testable)))
                  {:kaocha.error/reason         :kaocha.error/missing-method
                   :kaocha.error/missing-method `run-testable
                   :kaocha/testable             testable})))

(defn diff-test-result [before after]
  {:kaocha.result/pass (apply - (map :pass [after before]))
   :kaocha.result/error (apply - (map :error [after before]))
   :kaocha.result/fail (apply - (map :fail [after before]))})

(defmethod run-testable :kaocha.type/var [{:kaocha.var/keys [var test] :as testable}]
  (let [initial-report @t/*report-counters*]
    (binding [t/*testing-vars* (conj t/*testing-vars* var)]
      (t/do-report {:type :begin-test-var, :var var})
      (try
        (test)
        (catch clojure.lang.ExceptionInfo e
          (when-not (:kaocha/fail-fast (ex-data e))
            (t/do-report {:type :error, :message "Uncaught exception, not in assertion."
                          :expected nil, :actual e})))
        (catch Throwable e
          (t/do-report {:type :error, :message "Uncaught exception, not in assertion."
                        :expected nil, :actual e})))
      (t/do-report {:type :end-test-var, :var var}))
    (merge testable
           {:kaocha.result/count 1}
           (diff-test-result initial-report @t/*report-counters*))))

(s/fdef load-testable
        :args (s/cat :testable :kaocha/testable)
        :ret :kaocha.test-plan/testable)

(ns kaocha.report
  (:require [kaocha.output :as out :refer [colored]]
            [clojure.test :as t]
            [slingshot.slingshot :refer [throw+]]))

(def clojure-test-report t/report)

(defmethod t/report :begin-test-suite [m]
  (println "Test suite" (:kaocha.testable/id m)))

(defmethod t/report :end-test-suite [_])

(defn dispatch-extra-keys
  "Call the original clojure.test/report multimethod when dispatching an unknown
  key. This is to support libraries like nubank/matcher-combinators that extend
  clojure.test/assert-expr, as well as clojure.test/report, to signal special
  conditions."
  [m]
  (when-not (contains? #{:pass
                         :fail
                         :error
                         :begin-test-suite
                         :end-test-suite
                         :begin-test-ns
                         :end-test-ns
                         :begin-test-var
                         :end-test-var
                         :summary}
                       (:type m))
    (clojure-test-report m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti dots :type)
(defmethod dots :default [_])
(defmethod dots :pass [_] (print ".") (flush))
(defmethod dots :fail [_] (print (colored :red "F")) (flush))
(defmethod dots :error [_] (print (colored :red "E")) (flush))
(defmethod dots :end-test-suite [_] (println) (flush))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti report-counters :type)

(defmethod report-counters :default [_])

(defmethod report-counters :pass [m]
  (t/inc-report-counter :pass))

(defmethod report-counters :fail [m]
  (t/inc-report-counter :fail))

(defmethod report-counters :error [m]
  (t/inc-report-counter :error))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *results* nil)

(defmulti track :type)

(defmethod track :default [m] (swap! *results* conj m))

(defmethod track :pass [m]
  (swap! *results* conj m))

(defmethod track :fail [m]
  (swap! *results* conj (assoc m
                               :testing-contexts t/*testing-contexts*
                               :testing-vars t/*testing-vars*)) )

(defmethod track :error [m]
  (swap! *results* conj (assoc m
                               :testing-contexts t/*testing-contexts*
                               :testing-vars t/*testing-vars*)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti result :type)
(defmethod result :default [_])

(defmethod result :summary [m]
  (let [failures (filter (comp #{:fail :error} :type) @*results*)]
    (doseq [{:keys [testing-contexts testing-vars] :as m} failures]
      (binding [t/*testing-contexts* testing-contexts
                t/*testing-vars* testing-vars]
        (clojure-test-report m))))

  (let [{:keys [test pass fail error] :or {pass 0 fail 0 error 0}} m
        passed? (pos-int? (+ fail error))]
    (println (out/colored (if passed? :red :green)
                          (str test " test vars, "
                               (+ pass fail error) " assertions, "
                               (when (pos-int? error)
                                 (str error " errors, "))
                               fail " failures.")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fail-fast
  "Fail fast reporter, add this as a final reporter to interrupt testing as soon
  as a failure or error is encountered."
  [m]
  (when (= :fail (:type m))
    (throw+ {:kaocha/fail-fast true})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def doc-printed-contexts (atom nil))

(defn doc-print-contexts [contexts]
  (let [printed-contexts @doc-printed-contexts]
    (when (> (count contexts) (count printed-contexts))
      (doseq [[c1 c2] (map vector (concat printed-contexts
                                          (repeat nil)) contexts)]
        (print (if (= c1 c2)
                 "  "
                 (str "    " c2 "\n")))))
    (reset! doc-printed-contexts contexts)))

(defmulti doc :type)
(defmethod doc :default [_])

(defmethod doc :begin-test-ns [m]
  (reset! doc-printed-contexts (list))
  (println "Testing" (str (:ns m))))

(defmethod doc :end-test-ns [m]
  (println))

(defmethod doc :begin-test-var [m]
  (let [{:keys [name]} (-> m :var meta)]
    (println (str "  " name))))

(defmethod doc :pass [m] (doc-print-contexts t/*testing-contexts*))
(defmethod doc :error [m] (doc-print-contexts t/*testing-contexts*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn null [m])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def progress
  "Reporter that prints progress as a sequence of dots and letters."
  [report-counters track dispatch-extra-keys dots result])

(def documentation
  [report-counters track dispatch-extra-keys doc result])

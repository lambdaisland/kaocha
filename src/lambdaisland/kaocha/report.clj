(ns lambdaisland.kaocha.report
  (:require [lambdaisland.kaocha.output :as out :refer [colored]]
            [lambdaisland.kaocha :as k]
            [clojure.test :as t]
            [slingshot.slingshot :refer [throw+]]))

(def clojure-test-report t/report)

(defmethod t/report :begin-test-suite [m]
  (println "Test suite" (:id m)))

(defmethod t/report :end-test-suite [_])

(defmulti dots :type)
(defmethod dots :default [_])
(defmethod dots :pass [_] (print ".") (flush))
(defmethod dots :fail [_] (print (colored :red "F")) (flush))
(defmethod dots :error [_] (print (colored :red "E")) (flush))

(def ^:dynamic *results* nil)

(defmulti track :type)
(defmethod track :default [_])

(defmethod track :pass [_] (t/inc-report-counter :pass))

(defmethod track :fail [m]
  (t/inc-report-counter :fail)
  (swap! *results* update :failures conj (assoc m
                                                :testing-contexts t/*testing-contexts*
                                                :testing-vars t/*testing-vars*)) )

(defmethod track :error [m]
  (t/inc-report-counter :error)
  (swap! *results* update :errors conj (assoc m
                                              :testing-contexts t/*testing-contexts*
                                              :testing-vars t/*testing-vars*)))

(defmulti result :type)
(defmethod result :default [_])

(defmethod result :summary [m]
  (let [{:keys [failures errors]} @*results*]
    (doseq [{:keys [testing-contexts testing-vars] :as m} (concat errors failures)]
      (binding [t/*testing-contexts* testing-contexts
                t/*testing-vars* testing-vars]
        (clojure-test-report m))))

  (let [{:keys [pass fail error] :or {pass 0 fail 0 error 0}} m
        passed? (pos-int? (+ fail error))]
    (println (out/colored (if passed? :red :green)
                          (str
                           (if passed? "\n" "\n\n")
                           (+ pass fail error) " test vars, "
                           (when (pos-int? error)
                             (str error " errors, "))
                           fail " failures.")))))

(defn fail-fast
  "Fail fast reporter, add this as a final reporter to interrupt testing as soon
  as a failure or error is encountered."
  [m]
  (when (and (= :end-test-var (:type m)))
    (let [{:keys [fail error] :as report-counters} @t/*report-counters*]
      (when (or (> fail 0) (> error 0))
        (t/report {:type :end-test-ns :ns (-> m :var meta :ns)})
        (throw+ (assoc m
                       ::k/fail-fast true
                       ::k/report-counters report-counters))))))

(def progress
  "Reporter that prints progress as a sequence of dots and letters."
  [track dots result])

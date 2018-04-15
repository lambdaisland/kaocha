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
(defmethod dots :end-test-suite [_] (println) (flush))

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

(def progress
  "Reporter that prints progress as a sequence of dots and letters."
  [track dots result])

(def documentation
  [track doc result])

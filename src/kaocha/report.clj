(ns kaocha.report
  (:require [kaocha.output :as out :refer [colored]]
            [kaocha.stacktrace :as stack]
            [clojure.test :as t]
            [slingshot.slingshot :refer [throw+]]
            [clojure.string :as str]
            [kaocha.history :as history]))

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

(defmulti dots* :type)
(defmethod dots* :default [_])
(defmethod dots* :pass [_] (print ".") (flush))
(defmethod dots* :fail [_] (print (colored :red "F")) (flush))
(defmethod dots* :error [_] (print (colored :red "E")) (flush))
(defmethod dots* :begin-test-suite [_] (print "<") (flush))
(defmethod dots* :end-test-suite [_] (print "> ") (flush))

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

(defmulti result :type)
(defmethod result :default [_])

(defn- testing-vars-str
  "Returns a string representation of the current test. Renders names
  in :testing-vars as a list, then the source file and line of current
  assertion."
  [{:keys [file line testing-vars kaocha/testable] :as m}]
  (str
   ;; Uncomment to include namespace in failure report:
   ;;(ns-name (:ns (meta (first *testing-vars*)))) "/ "
   (if (seq testing-vars)
     (reverse (map #(:name (meta %)) testing-vars))
     (name (:kaocha.testable/id testable)))
   " (" file ":" line ")"))

(defn- summary-fail [{:keys [testing-contexts testing-vars] :as m}]
  (t/with-test-out
    (println "\nFAIL in" (testing-vars-str m))
    (when (seq testing-contexts)
      (println (str/join " " testing-contexts)))
    (when-let [message (:message m)]
      (println message))
    (println "expected:" (pr-str (:expected m)))
    (println "  actual:" (pr-str (:actual m)))))

(defn- summary-error [{:keys [testing-contexts testing-vars] :as m}]
  (t/with-test-out
    (println "\nERROR in" (testing-vars-str m))
    (when (seq testing-contexts)
      (println (str/join " " testing-contexts)))
    (when-let [message (:message m)]
      (println message))
    (print "Exception: ")
    (let [actual (:actual m)]
      (if (instance? Throwable actual)
        (stack/print-cause-trace actual t/*stack-trace-depth*)
        (prn actual)))))

(defmethod result :summary [m]
  (let [failures (filter (comp #{:fail :error} :type) @history/*history*)]
    (doseq [{:keys [testing-contexts testing-vars] :as m} failures]
      (binding [t/*testing-contexts* testing-contexts
                t/*testing-vars* testing-vars]
        (case (:type m)
          :fail (summary-fail m)
          :error (summary-error m)))))

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
  (when (and (some #{(:type m)} [:error :fail :mismatch])
             (not (:kaocha.result/exception m))) ;; prevent handled exceptions from being re-thrown
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
  (t/with-test-out
    (reset! doc-printed-contexts (list))
    (println "Testing" (-> m :kaocha/testable :kaocha.ns/name))))

(defmethod doc :end-test-ns [m]
  (t/with-test-out
    (println)))

(defmethod doc :begin-test-var [m]
  (t/with-test-out
    (let [{:keys [name]} (-> m :var meta)]
      (println (str "  " name)))))

(defmethod doc :pass [m]
  (t/with-test-out
    (doc-print-contexts t/*testing-contexts*)))

(defmethod doc :error [m]
  (t/with-test-out
    (doc-print-contexts t/*testing-contexts*)))

(defn debug [m]
  (prn (cond-> (select-keys m [:type :var :ns])
         (:kaocha/testable m) (update :kaocha/testable select-keys [:kaocha.testable/id :kaocha.testable/type]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def dots
  "Reporter that prints progress as a sequence of dots and letters."
  [dots* result])

(def documentation
  [doc result])

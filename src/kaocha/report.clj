(ns kaocha.report
  (:require [kaocha.output :as out]
            [kaocha.plugin.capture-output :as capture]
            [kaocha.stacktrace :as stack]
            [clojure.test :as t]
            [slingshot.slingshot :refer [throw+]]
            [clojure.string :as str]
            [kaocha.history :as history]
            [kaocha.testable :as testable]))

(defonce hierarchy (make-hierarchy))

(defn derive! [tag parent]
  (alter-var-root #'hierarchy derive tag parent))

(derive! :fail :kaocha/fail-type)
(derive! :error :kaocha/fail-type)

(derive! :pass :kaocha/known-key)
(derive! :fail :kaocha/known-key)
(derive! :error :kaocha/known-key)
(derive! :begin-test-suite :kaocha/known-key)
(derive! :end-test-suite :kaocha/known-key)
(derive! :begin-test-ns :kaocha/known-key)
(derive! :end-test-ns :kaocha/known-key)
(derive! :begin-test-var :kaocha/known-key)
(derive! :end-test-var :kaocha/known-key)
(derive! :summary :kaocha/known-key)

(def clojure-test-report t/report)

(defn dispatch-extra-keys
  "Call the original clojure.test/report multimethod when dispatching an unknown
  key. This is to support libraries like nubank/matcher-combinators that extend
  clojure.test/assert-expr, as well as clojure.test/report, to signal special
  conditions."
  [m]
  (when-not (isa? hierarchy (:type m) :kaocha/known-key)
    (clojure-test-report m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti dots* :type)
(defmethod dots* :default [_])

(defmethod dots* :pass [_]
  (t/with-test-out
    (print ".")
    (flush)))

(defmethod dots* :fail [_]
  (t/with-test-out
    (print (out/colored :red "F"))
    (flush)))

(defmethod dots* :error [_]
  (t/with-test-out
    (print (out/colored :red "E"))
    (flush)))

(defmethod dots* :begin-test-ns [_]
  (t/with-test-out
    (print "(")
    (flush)))

(defmethod dots* :end-test-ns [_]
  (t/with-test-out
    (print ")")
    (flush)))

(defmethod dots* :begin-test-suite [_]
  (t/with-test-out
    (print "[")
    (flush)))

(defmethod dots* :end-test-suite [_]
  (t/with-test-out
    (print "]")
    (flush)))

(defmethod dots* :summary [_]
  (t/with-test-out
    (println)))

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
  (let [file (or (some-> testable ::testable/meta :file) file)
        line (or (some-> testable ::testable/meta :line) line)]
    (str
     ;; Uncomment to include namespace in failure report:
     ;;(ns-name (:ns (meta (first *testing-vars*)))) "/ "
     (if (seq testing-vars)
       (reverse (map #(:name (meta %)) testing-vars))
       (name (:kaocha.testable/id testable)))
     " (" file ":" line ")")))

(defn print-output [m]
  (let [buffer (get-in m [:kaocha/testable ::capture/buffer])
        out (capture/read-buffer buffer)]
    (when (seq out)
      (println "------ Test output -------------------------------------")
      (println (str/trim-newline out))
      (println "--------------------------------------------------------"))))

(defmulti fail-summary :type)

(defmethod fail-summary :fail [{:keys [testing-contexts testing-vars] :as m}]
  (println "\nFAIL in" (testing-vars-str m))
  (when (seq testing-contexts)
    (println (str/join " " testing-contexts)))
  (when-let [message (:message m)]
    (println message))
  (println "expected:" (pr-str (:expected m)))
  (println "  actual:" (pr-str (:actual m)))
  (print-output m))

(defmethod fail-summary :error [{:keys [testing-contexts testing-vars] :as m}]
  (println "\nERROR in" (testing-vars-str m))
  (when (seq testing-contexts)
    (println (str/join " " testing-contexts)))
  (when-let [message (:message m)]
    (println message))
  (print-output m)
  (print "Exception: ")
  (let [actual (:actual m)]
    (if (instance? Throwable actual)
      (stack/print-cause-trace actual t/*stack-trace-depth*)
      (prn actual))))

(defmethod result :summary [m]
  (t/with-test-out
    (let [failures (filter #(isa? hierarchy (:type %) :kaocha/fail-type) @history/*history*)]
      (doseq [{:keys [testing-contexts testing-vars] :as m} failures]
        (binding [t/*testing-contexts* testing-contexts
                  t/*testing-vars* testing-vars]
          (fail-summary m))))

    (let [{:keys [test pass fail error] :or {pass 0 fail 0 error 0}} m
          passed? (pos-int? (+ fail error))]
      (println (out/colored (if passed? :red :green)
                            (str test " test vars, "
                                 (+ pass fail error) " assertions, "
                                 (when (pos-int? error)
                                   (str error " errors, "))
                                 fail " failures."))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fail-fast
  "Fail fast reporter, add this as a final reporter to interrupt testing as soon
  as a failure or error is encountered."
  [m]
  (when (and (isa? hierarchy (:type m) :kaocha/fail-type)
             (not (:kaocha.result/exception m))) ;; prevent handled exceptions from being re-thrown
    (throw+ {:kaocha/fail-fast true})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def doc-printed-contexts (atom nil))

(defn doc-print-contexts [contexts & [suffix]]
  (let [printed-contexts @doc-printed-contexts]
    (let [contexts     (reverse contexts)
          printed      (reverse printed-contexts)
          pairwise     (map vector (concat printed (repeat nil)) contexts)
          nesting      (->> pairwise (take-while (fn [[x y]] (= x y))) count)
          new-contexts (->> pairwise (drop-while (fn [[x y]] (= x y))) (map last))]
      (when (seq new-contexts)
        (doseq [[ctx idx] (map vector new-contexts (range))
                :let [nesting (+ nesting idx)]]
          (print (str "\n"
                      "    "
                      (apply str (repeat nesting "  "))
                      ctx))
          (flush))))

    #_(when (> (count contexts) (count printed-contexts))


      (println)
      (doseq [[c1 c2] (map vector (concat printed-contexts
                                          (repeat nil)) contexts)]
        (print (if (= c1 c2)
                 "  "
                 (str "    " c2)))))
    (reset! doc-printed-contexts contexts)))

(defmulti doc :type)
(defmethod doc :default [_])

(defmethod doc :begin-test-suite [m]
  (t/with-test-out
    (reset! doc-printed-contexts (list))
    (print "---" (-> m :kaocha/testable :kaocha.testable/id) "---------------------------")
    (flush)))

(defmethod doc :begin-test-ns [m]
  (t/with-test-out
    (reset! doc-printed-contexts (list))
    (print (str "\n" (-> m :kaocha/testable :kaocha.ns/name)))
    (flush)))

(defmethod doc :end-test-ns [m]
  (t/with-test-out
    (println)))

(defmethod doc :begin-test-var [m]
  (t/with-test-out
    (let [{:keys [name]} (-> m :var meta)]
      (print (str "\n  " name))
      (flush))))

(defmethod doc :pass [m]
  (t/with-test-out
    (doc-print-contexts t/*testing-contexts*)))

(defmethod doc :error [m]
  (t/with-test-out
    (doc-print-contexts t/*testing-contexts*)
    (print (out/colored :red " ERROR"))))

(defmethod doc :fail [m]
  (t/with-test-out
    (doc-print-contexts t/*testing-contexts*)
    (print (out/colored :red " FAIL"))))

(defmethod doc :summary [m]
  (t/with-test-out
    (println)))

(defn debug [m]
  (prn (cond-> (select-keys m [:type :var :ns])
         (:kaocha/testable m) (update :kaocha/testable select-keys [:kaocha.testable/id :kaocha.testable/type]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def dots
  "Reporter that prints progress as a sequence of dots and letters."
  [dots* result])

(def documentation
  [doc result])

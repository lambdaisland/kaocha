(ns lambdaisland.kaocha.test
  (:require [clojure.test :as t]
            [lambdaisland.kaocha.report :as report]
            [lambdaisland.kaocha.load :as load]
            [lambdaisland.kaocha.output :as output]
            [lambdaisland.kaocha.config :as config]
            [slingshot.slingshot :refer [try+ throw+]]
            [lambdaisland.kaocha :as k]
            [clojure.string :as str]))

(def ^:private empty-report {:test 0 :pass 0 :fail 0 :error 0})

(defn- merge-report [r1 r2]
  (merge-with #(if (int? %1) (+ %1 %2) %2) empty-report r1 r2))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clojure.test functions

(defn- stacktrace-file-and-line [stacktrace]
  (if (seq stacktrace)
    (let [^StackTraceElement s (first stacktrace)]
      {:file (.getFileName s) :line (.getLineNumber s)})
    {:file nil :line nil}))

;; This is an unfortunate hack. clojure.test/is wraps all assertions in a
;; try/catch, but we actually want to use an exception to signal a failure when
;; using --fail-fast, so we can skip the rest of the assertions in the var. This
;; detects our own fail-fast exception, and rethrows it, rather than reporting
;; it as an error.
(alter-var-root #'clojure.test/do-report
                (fn [_]
                  (fn [m]
                    (t/report
                     (case (:type m)
                       :fail
                       (merge (stacktrace-file-and-line (drop-while
                                                         #(let [cl-name (.getClassName ^StackTraceElement %)]
                                                            (or (str/starts-with? cl-name "java.lang.")
                                                                (str/starts-with? cl-name "clojure.test$")
                                                                (str/starts-with? cl-name "lambdaisland.kaocha.test$")))
                                                         (.getStackTrace (Thread/currentThread)))) m)
                       :error
                       (if (-> m :actual ex-data ::k/fail-fast)
                         (throw (:actual m))
                         (merge (stacktrace-file-and-line (.getStackTrace ^Throwable (:actual m))) m))
                       m)))))

(defn test-var
  "If v has a function in its :test metadata, calls that function,
  with *testing-vars* bound to (conj *testing-vars* v)."
  [v]
  (when-let [t (:test (meta v))]
    (binding [t/*testing-vars* (conj t/*testing-vars* v)]
      (t/do-report {:type :begin-test-var, :var v})
      (t/inc-report-counter :test)
      (try+
       (t)
       (catch ::k/fail-fast m
         (t/do-report {:type :end-test-var, :var v})
         (throw+ m))
       (catch Throwable e
         (t/do-report {:type :error, :message "Uncaught exception, not in assertion."
                       :expected nil, :actual e})))
      (t/do-report {:type :end-test-var, :var v}))))

(defn test-vars
  "Groups vars by their namespace and runs test-vars on them with
   appropriate fixtures applied."
  [vars]
  (doseq [[ns vars] (group-by (comp :ns meta) vars)]
    (let [once-fixture-fn (t/join-fixtures (::once-fixtures (meta ns)))
          each-fixture-fn (t/join-fixtures (::each-fixtures (meta ns)))]
      (once-fixture-fn
       (fn []
         (doseq [v vars]
           (when (:test (meta v))
             (each-fixture-fn (fn [] (test-var v))))))))))

(defn test-all-vars
  "Calls test-vars on every var interned in the namespace, with fixtures."
  [ns]
  (test-vars (vals (ns-interns ns))))

(defn test-ns
  "If the namespace defines a function named test-ns-hook, calls that.
  Otherwise, calls test-all-vars on the namespace.  'ns' is a
  namespace object or a symbol.

  Internally binds *report-counters* to a ref initialized to
  *initial-report-counters*.  Returns the final, dereferenced state of
  *report-counters*."
  [ns]
  (binding [t/*report-counters* (ref t/*initial-report-counters*)]
    (let [ns-obj (the-ns ns)]
      (t/do-report {:type :begin-test-ns, :ns ns-obj})
      ;; If the namespace has a test-ns-hook function, call that:
      (if-let [v (find-var (symbol (str (ns-name ns-obj)) "test-ns-hook"))]
	      ((var-get v))
        ;; Otherwise, just test every var in the namespace.
        (test-all-vars ns-obj))
      (t/do-report {:type :end-test-ns, :ns ns-obj}))
    @t/*report-counters*))

(defn try-test-ns [ns]
  (try+
   (test-ns ns)
   (catch ::k/fail-fast m
     m)))

(defn run-tests
  "Runs all tests in the given namespaces; prints results.
  Defaults to current namespace if none given.  Returns a map
  summarizing test results."
  ([]
   (run-tests *ns*))
  ([& namespaces]
   (loop [[ns & nss] namespaces
          report {}]
     (if ns
       (let [ns-report (try-test-ns ns)]
         (if (::k/fail-fast ns-report)
           (recur [] (merge-report report ns-report))
           (recur nss (merge-report report ns-report))))
       report))))

(defn run-all-tests
  "Runs all tests in all namespaces; prints results.
  Optional argument is a regular expression; only namespaces with
  names matching the regular expression (with re-matches) will be
  tested."
  ([] (apply run-tests (all-ns)))
  ([re] (apply run-tests (filter #(re-matches re (name (ns-name %))) (all-ns)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro ^:private with-reporter [r & body]
  `(with-redefs [t/report (config/resolve-reporter ~r)]
     ~@body))


(defn- run-suite [{:keys [nss] :as suite}]
  (t/do-report (assoc suite :type :begin-test-suite))
  (let [report (apply run-tests nss)]
    (t/do-report (assoc suite :type :end-test-suite))
    report))

(defn result->report [results]
  (reduce (fn [r {type :type}]
            (cond
              (contains? #{:fail :pass :error} type) (update r type inc)
              (= :begin-test-var type)               (update r :test inc)
              :else                                  r))
          {:test 0 :pass 0 :fail 0 :error 0}
          results))

(defn run [config]
  (let [{:keys [reporter
                color
                suites
                only-suites
                fail-fast]} (config/normalize config)
        suites              (config/filter-suites only-suites suites)
        reporter            (if fail-fast
                              [reporter report/fail-fast]
                              reporter)
        results             (atom [])
        runtime             (java.lang.Runtime/getRuntime)
        main-thread         (Thread/currentThread)
        on-shutdown         (Thread. (fn []
                                       (println "^C")
                                       (binding [report/*results* results]
                                         (t/do-report (assoc (result->report @results)
                                                             :type :summary)))))
        do-finish           (fn [report]
                              (t/do-report (assoc report :type :summary))
                              (.removeShutdownHook runtime on-shutdown)
                              report)]
    (.addShutdownHook runtime on-shutdown)
    (try
      (with-reporter reporter
        (binding [output/*colored-output* color
                  report/*results*        results]
          (let [suites (map load/find-tests suites)]
            (loop [[suite & suites] suites
                   report           {}]
              (if suite
                (let [report (merge-report report (run-suite suite))]
                  (if (::k/fail-fast report)
                    (do
                      (recur [] report))
                    (recur suites report)))
                (do-finish report))))))
      (finally
        (.removeShutdownHook runtime on-shutdown)))))

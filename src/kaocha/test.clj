(ns kaocha.test
  (:require [clojure.test :as t]
            [kaocha.report :as report]
            [kaocha.load :as load]
            [kaocha.output :as output]
            [kaocha.random :as random]
            [kaocha.config :as config]
            [slingshot.slingshot :refer [try+ throw+]]
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
                                                                (str/starts-with? cl-name "kaocha.test$")))
                                                         (.getStackTrace (Thread/currentThread)))) m)
                       :error
                       (if (-> m :actual ex-data :kaocha/fail-fast)
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
       (catch :kaocha/fail-fast m
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

(defn test-ns [ns vars]
  (binding [t/*report-counters* (ref t/*initial-report-counters*)]
    (let [ns-obj (the-ns ns)]
      (t/do-report {:type :begin-test-ns, :ns ns-obj})
      ;; If the namespace has a test-ns-hook function, call that:
      (if-let [v (find-var (symbol (str (ns-name ns-obj)) "test-ns-hook"))]
	      ((var-get v))
        ;; Otherwise, just test every var in the namespace.
        (test-vars vars))
      (t/do-report {:type :end-test-ns, :ns ns-obj}))
    @t/*report-counters*))

(defn try-test-ns [ns+vars]
  (try+
   (apply test-ns ns+vars)
   (catch :kaocha/fail-fast m
     m)))

(defn run-tests [namespaces]
  (loop [[ns+vars & nss] namespaces
         report {}]
    (if ns+vars
      (let [ns-report (try-test-ns ns+vars)]
        (if (:kaocha/fail-fast ns-report)
          (recur [] (merge-report report ns-report))
          (recur nss (merge-report report ns-report))))
      report)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro ^:private with-reporter [r & body]
  `(with-redefs [t/report ~r]
     ~@body))

(defn- run-suite [{:kaocha/keys [tests] :as suite}]
  (t/do-report (assoc suite :type :begin-test-suite))
  (let [report (run-tests tests)]
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
  (let [{:kaocha/keys [reporter
                       color?
                       suites
                       only-suites
                       fail-fast?
                       seed
                       randomize?]} (config/normalize config)
        seed                (or seed (rand-int Integer/MAX_VALUE))
        suites              (config/filter-suites only-suites suites)
        reporter            (config/resolve-reporter
                             (if fail-fast?
                               [reporter report/fail-fast]
                               reporter))
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
    (when randomize?
      (println "Running with --seed" seed))
    (try
      (binding [output/*colored-output* color?
                report/*results*        results]
        (let [suites (doall
                      (cond->> (map load/find-tests suites)
                        randomize? (map #(update % :kaocha/tests (partial random/randomize-tests seed)))))]
          (with-reporter reporter
            (loop [[suite & suites] suites
                   report           {}]
              (if suite
                (let [report (merge-report report (run-suite suite))]
                  (if (:kaocha/fail-fast report)
                    (do
                      (recur [] report))
                    (recur suites report)))
                (do-finish report))))))
      (finally
        (.removeShutdownHook runtime on-shutdown)))))

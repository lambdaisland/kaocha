(ns lambdaisland.kaocha.test
  (:require [clojure.test :as t]
            [lambdaisland.kaocha.report :as report]
            [lambdaisland.kaocha.load :as load]
            [lambdaisland.kaocha.output :as output]
            [lambdaisland.kaocha.config :as config]
            [slingshot.slingshot :refer [try+]]
            [lambdaisland.kaocha :as k]))

(defmacro ^:private with-reporter [r & body]
  `(with-redefs [t/report (config/resolve-reporter ~r)]
     ~@body))

(def ^:private empty-report {:pass 0 :fail 0 :error 0})

(defn- merge-report [r1 r2]
  (merge-with #(if (int? %1) (+ %1 %2) %2) empty-report r1 r2))

(defn- try-test-ns [ns]
  (try+
   (t/test-ns ns)
   (catch ::k/fail-fast m
     m)))

(defn- run-suite [{:keys [color] :as suite}]
  (t/do-report (assoc suite :type :begin-test-suite))
  (loop [[ns & nss] (:nss suite)
         report {}]
    (if ns
      (let [ns-report (try-test-ns ns)]
        (if (::k/fail-fast ns-report)
          (do
            (t/do-report (assoc suite :type :end-test-suite))
            (assoc (merge-report report (::k/report-counters ns-report))
                   ::k/fail-fast true))
          (recur nss (merge-report report ns-report))))
      (do
        (t/do-report (assoc suite :type :end-test-suite))
        report))))

(defn run [config]
  (let [{:keys [reporter
                color
                suites
                only-suites
                fail-fast]} (config/normalize config)
        suites              (config/filter-suites only-suites suites)
        reporter            (if fail-fast
                              [reporter report/fail-fast]
                              reporter)]
    (with-reporter reporter
      (binding [output/*colored-output* color
                report/*results* (atom {})]
        (let [suites (map load/find-tests suites)]
          (loop [[suite & suites] suites
                 report           {}]
            (if suite
              (let [report (merge-report report (run-suite suite))]
                (if (::k/fail-fast report)
                  (do
                    (t/do-report (assoc report :type :summary))
                    report)
                  (recur suites report)))
              (do
                (t/do-report (assoc report :type :summary))
                report))))))))

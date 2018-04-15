(ns lambdaisland.kaocha.test
  (:require [clojure.test :as t]
            [lambdaisland.kaocha.report :as report]
            [lambdaisland.kaocha.load :as load]
            [lambdaisland.kaocha.output :as output]
            [lambdaisland.kaocha.config :as config]))

(defmacro with-reporter [r & body]
  `(with-redefs [t/report (config/resolve-reporter ~r)]
     ~@body))

(defn- run-suite [{:keys [color] :as suite}]
  (try
    (t/do-report (assoc suite :type :begin-test-suite))
    (let [result (doall (map t/test-ns (:test-nss suite)))]
      (t/do-report (assoc suite :type :end-test-suite))
      result)
    (catch Exception e
      (prn e))))

(defn run-suites [config]
  (let [{:keys [reporter color suites only-suites]} (config/normalize config)
        suites (config/filter-suites only-suites suites)]
    (binding [output/*colored-output* color]
      (with-reporter reporter
        (binding [report/*results* (atom {})]
          (let [suites (map #(assoc % :test-nss (load/load-tests %)) suites)
                result (apply merge-with #(if (int? %1) (+ %1 %2) %2) (mapcat run-suite suites))]
            (t/do-report (assoc result :type :summary))
            result))))))

(ns lambdaisland.kaocha.test
  (:require [clojure.test :as t]
            [lambdaisland.kaocha.report :as report]
            [lambdaisland.kaocha.load :as load]
            [lambdaisland.kaocha.output :as output]))


(defmacro with-reporter [r & body]
  `(with-redefs [t/report ~r]
     ~@body))

(defn- run-suite [{:keys [color] :as suite}]
  (binding [output/*colored-output* color]
    (t/do-report (assoc suite :type :begin-test-suite))
    (let [result (doall (map t/test-ns (:test-nss suite)))]
      (t/do-report (assoc suite :type :end-test-suite))
      result)))

(defn run-suites [reporter suites]
  (with-reporter reporter
    (binding [report/*results* (atom {})]
      (let [suites (map #(assoc % :test-nss (load/load-tests %)) suites)
            result (apply merge-with #(if (int? %1) (+ %1 %2) %2) (mapcat run-suite suites))]
        (t/do-report (assoc result :type :summary))))))

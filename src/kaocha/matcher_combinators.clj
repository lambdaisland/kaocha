(ns kaocha.matcher-combinators
  (:require [kaocha.report :as report]
            [kaocha.output :as out]
            [clojure.test :as t]))

(report/derive! :mismatch :kaocha/fail-type)
(report/derive! :mismatch :kaocha/known-key)

(report/derive! :matcher-combinators/mismatch :kaocha/fail-type)
(report/derive! :matcher-combinators/mismatch :kaocha/known-key)

;; newer versions of matcher-combinators
(defmethod report/dots* :matcher.combinators/mismatch [_]
  (t/with-test-out
    (print (out/colored :red "F"))
    (flush)))

;; older versions of matcher-combinators
(defmethod report/dots* :mismatch [_]
  (t/with-test-out
    (print (out/colored :red "F"))
    (flush)))

(defn fail-summary [{:keys [testing-contexts testing-vars] :as m}]
  (println "\nFAIL in" (clojure.test/testing-vars-str m))
  (when (seq t/*testing-contexts*)
    (println (t/testing-contexts-str)))
  (when-let [message (:message m)]
    (println message))
  (println "mismatch:")
  ((resolve 'matcher-combinators.printer/pretty-print) (:markup m))
  (report/print-output m))

(defmethod report/fail-summary :mismatch [m] (fail-summary m))
(defmethod report/fail-summary :matcher-combinators/mismatch [m] (fail-summary m))

(defmethod report/report-counters :mismatch [m]
  (t/inc-report-counter :fail))

(defmethod report/report-counters :matcher-combinators/mismatch [m]
  (t/inc-report-counter :fail))

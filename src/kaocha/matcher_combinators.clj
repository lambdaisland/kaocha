(ns kaocha.matcher-combinators
  (:require [kaocha.report :as report]
            [kaocha.output :as out]
            [kaocha.hierarchy :as hierarchy]
            [clojure.test :as t]))

(hierarchy/derive! :mismatch :kaocha/fail-type)
(hierarchy/derive! :mismatch :kaocha/known-key)

(hierarchy/derive! :matcher-combinators/mismatch :kaocha/fail-type)
(hierarchy/derive! :matcher-combinators/mismatch :kaocha/known-key)

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

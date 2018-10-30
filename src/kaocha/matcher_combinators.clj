(ns kaocha.matcher-combinators
  (:require [kaocha.report :as report]
            [kaocha.output :as out]
            [kaocha.hierarchy :as hierarchy]
            [clojure.test :as t]
            [lambdaisland.deep-diff.printer :as printer]
            [puget.color :as color]))

(hierarchy/derive! :mismatch :kaocha/fail-type)
(hierarchy/derive! :mismatch :kaocha/known-key)

(hierarchy/derive! :matcher-combinators/mismatch :kaocha/fail-type)
(hierarchy/derive! :matcher-combinators/mismatch :kaocha/known-key)

(def print-handlers {'matcher_combinators.model.Mismatch
                     (fn [printer expr]
                       (printer/print-mismatch printer {:- (:expected expr)
                                                        :+ (:actual expr)}))

                     'matcher_combinators.model.Missing
                     (fn [printer expr]
                       (printer/print-deletion printer {:- (:expected expr)}))

                     'matcher_combinators.model.Unexpected
                     (fn [printer expr]
                       (printer/print-insertion printer {:+ (:actual expr)}))

                     'matcher_combinators.model.FailedPredicate
                     (fn [printer expr]
                       [:group
                        [:align
                         (printer/print-other printer (:form expr))
                         (printer/print-insertion printer {:+ (:actual expr)})]])

                     'matcher_combinators.model.InvalidMatcherType
                     (fn [printer expr]
                       [:group
                        [:align
                         (color/document printer
                                         ::printer/other
                                         [:span "-"
                                          [:raw (:expected-type-msg expr)]])
                         (printer/print-insertion printer {:+ (:provided expr)})]])})

(run! #(apply printer/register-print-handler! %) print-handlers)

(defn fail-summary [{:keys [testing-contexts testing-vars] :as m}]
  (println "\nFAIL in" (clojure.test/testing-vars-str m))
  (when (seq t/*testing-contexts*)
    (println (t/testing-contexts-str)))
  (when-let [message (:message m)]
    (println message))
  (printer/print-doc
   [:span
    "Mismatch:" :line
    [:nest (printer/format-doc (:markup m))]])
  (report/print-output m))

(defmethod report/fail-summary :mismatch [m] (fail-summary m))
(defmethod report/fail-summary :matcher-combinators/mismatch [m] (fail-summary m))

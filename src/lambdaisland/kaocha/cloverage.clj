(ns lambdaisland.kaocha.cloverage
  (:require [cloverage.coverage :as c]
            [lambdaisland.kaocha.test :as t]
            [lambdaisland.kaocha.runner :as runner]))

(defmethod c/runner-fn :lambdaisland.kaocha [_]
  (fn [_]
    (merge {:errors 0}
           (t/run-suites (runner/config {})))))

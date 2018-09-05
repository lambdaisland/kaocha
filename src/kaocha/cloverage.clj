(ns kaocha.cloverage
  (:require [cloverage.coverage :as c]
            [kaocha.runner :as runner]))

(defmethod c/runner-fn :kaocha [_]
  (fn [_]
    (merge {:errors 0}
           (t/run (runner/config {})))))

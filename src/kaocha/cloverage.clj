(ns kaocha.cloverage
  #_(:require [cloverage.coverage :as c]
              [kaocha.runner :as runner]))

#_
(defmethod c/runner-fn :kaocha [_]
  (fn [_]
    (merge {:errors 0}
           (t/run (runner/config {})))))

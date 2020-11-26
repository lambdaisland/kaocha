(ns kaocha.plugin.retry
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [kaocha.plugin :refer [defplugin]]
            [orchestra.spec.test :as orchestra]
            [clojure.spec.alpha :as spec]))

(defplugin kaocha.plugin/retry
  (post-run [result]
    (println "test was run with result" (keys result))
    (println "error = "
             (-> result
                 ;; :kaocha.result
                 :kaocha.result/tests
                 first
                 :kaocha.result/tests
                 ;; keys
                 ;; first
                 #_:kaocha.result/error)
             "fail ="
             #_(-> result
                 :kaocha.result/tests
                 ;; first
                 #_:kaocha.result/fail))
    result))

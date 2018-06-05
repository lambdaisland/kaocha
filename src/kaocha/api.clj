(ns kaocha.api
  "Programmable test runner interface."
  (:require [kaocha.monkey-patch]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            ))


(defn run [config])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

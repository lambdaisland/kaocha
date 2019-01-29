(ns kaocha.plugin.bindings
  (:require [kaocha.plugin :refer [defplugin]]))

(defplugin kaocha.plugin/bindings
  "Set dynamic bindings during the test run."
  (wrap-run [run test-plan]
    (if-let [bindings (:kaocha/bindings test-plan)]
      (let [bindings (into {} (map (fn [[k v]] [(find-var k) v])) bindings)]
        (fn [& args]
          (try
            (push-thread-bindings bindings)
            (apply run args)
            (finally
              (pop-thread-bindings)))))
      run)))

(ns kaocha.plugin.hooks
  (:require [kaocha.plugin :refer [defplugin]]))

(defn update? [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn load-hooks [hs]
  (mapv (fn [h]
          (cond
            (qualified-symbol? h)
            (do
              (require (symbol (namespace h)))
              (resolve h))

            (list? h)
            (eval h)

            :else
            h))
        hs))

(defplugin kaocha.plugin/hooks
  (config [config]
    (-> config
        (update? :kaocha.hooks/pre-load load-hooks)
        (update? :kaocha.hooks/post-load load-hooks)
        (update? :kaocha.hooks/pre-run load-hooks)
        (update? :kaocha.hooks/post-run load-hooks)
        (update? :kaocha.hooks/pre-test load-hooks)
        (update? :kaocha.hooks/post-test load-hooks)))

  (pre-load [config]
    (reduce #(%2 %1) config (:kaocha.hooks/pre-load config)))

  (post-load [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/post-load test-plan)))

  (pre-run [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/pre-run test-plan)))

  (post-run [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/post-run test-plan)))

  (pre-test [testable test-plan]
    (reduce #(%2 %1 test-plan) testable (:kaocha.hooks/pre-test test-plan)))

  (post-test [testable test-plan]
    (reduce #(%2 %1 test-plan) testable (:kaocha.hooks/post-test test-plan))))

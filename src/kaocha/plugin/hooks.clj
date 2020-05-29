(ns kaocha.plugin.hooks
  (:require [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]))

(defn update? [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn load-hook [h]
  (cond
    (qualified-symbol? h)
    (do
      (require (symbol (namespace h)))
      (resolve h))

    (list? h)
    (eval h)

    :else
    h))

(defn load-hooks [hs]
  (if (coll? hs)
    (mapv load-hook hs)
    [(load-hook hs)]))

(defn load-suite-hooks [suites]
  (map
   (fn [suite]
     (-> suite
         (update? :kaocha.hooks/before load-hooks)
         (update? :kaocha.hooks/after load-hooks)))
   suites))

(defplugin kaocha.plugin/hooks
  "Configure hooks directly in `tests.edn`."
  (config [config]
    (let [config (-> config
                     (update? :kaocha.hooks/config load-hooks)
                     (update? :kaocha.hooks/pre-load load-hooks)
                     (update? :kaocha.hooks/post-load load-hooks)
                     (update? :kaocha.hooks/pre-run load-hooks)
                     (update? :kaocha.hooks/post-run load-hooks)
                     (update? :kaocha.hooks/wrap-run load-hooks)
                     (update? :kaocha.hooks/pre-test load-hooks)
                     (update? :kaocha.hooks/post-test load-hooks)
                     (update? :kaocha.hooks/pre-report load-hooks)
                     (update? :kaocha/tests load-suite-hooks))]
      (reduce #(%2 %1) config (:kaocha.hooks/config config))))

  (pre-load [config]
    (reduce #(%2 %1) config (:kaocha.hooks/pre-load config)))

  (post-load [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/post-load test-plan)))

  (pre-run [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/pre-run test-plan)))

  (post-run [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/post-run test-plan)))

  (wrap-run [run test-plan]
    (reduce #(%2 %1) run (:kaocha.hooks/wrap-run test-plan)))

  (pre-test [testable test-plan]
    (as-> testable $
      (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/before testable))
      (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/pre-test test-plan))))

  (post-test [testable test-plan]
    (as-> testable $
      (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/post-test test-plan))
      (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/after testable))))

  (pre-report [event]
    (reduce #(%2 %1) event (:kaocha.hooks/pre-report testable/*test-plan*))))

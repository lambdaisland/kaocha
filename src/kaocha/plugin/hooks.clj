(ns kaocha.plugin.hooks
  (:require [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]))

(defn- update? [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defn- load-hook [h]
  (cond
    (qualified-symbol? h)
    (do
      (require (symbol (namespace h)))
      (resolve h))

    (list? h)
    (eval h)

    :else
    h))

(defn- load-hooks [hs]
  (if (vector? hs)
    (mapv load-hook hs)
    (load-hooks [hs])))

(defn- load-testable-level-hooks
  "Load hooks that fire per testable, these can be either on the individual
  testable, or on the top level config."
  [config-or-testable]
  (-> config-or-testable
      (update? :kaocha.hooks/pre-load-test load-hooks)
      (update? :kaocha.hooks/post-load-test load-hooks)
      (update? :kaocha.hooks/pre-test load-hooks)
      (update? :kaocha.hooks/post-test load-hooks)))

(defn- rename-aliases [config-or-testable]
  (cond-> config-or-testable
    (:kaocha.hooks/before config-or-testable)
    (->
     (update :kaocha.hooks/pre-test (fnil into []) (:kaocha.hooks/before config-or-testable))
     (dissoc :kaocha.hooks/before))
    (:kaocha.hooks/after config-or-testable)
    (->
     (update :kaocha.hooks/post-test (fnil into []) (:kaocha.hooks/after config-or-testable))
     (dissoc :kaocha.hooks/after))))

(defplugin kaocha.plugin/hooks
  "Configure hooks directly in `tests.edn`."
  (config [config]
    (let [config (-> config
                     rename-aliases
                     (update? :kaocha.hooks/config load-hooks)
                     (update? :kaocha.hooks/pre-load load-hooks)
                     (update? :kaocha.hooks/post-load load-hooks)
                     (update? :kaocha.hooks/pre-run load-hooks)
                     (update? :kaocha.hooks/post-run load-hooks)
                     (update? :kaocha.hooks/wrap-run load-hooks)
                     (update? :kaocha.hooks/pre-test load-hooks)
                     (update? :kaocha.hooks/post-test load-hooks)
                     (update? :kaocha.hooks/pre-report load-hooks)
                     (update? :kaocha.hooks/post-summary load-hooks)
                     load-testable-level-hooks
                     (update? :kaocha/tests #(mapv (comp load-testable-level-hooks rename-aliases) %)))]
      (reduce #(%2 %1) config (:kaocha.hooks/config config))))

  (pre-load [config]
    (reduce #(%2 %1) config (:kaocha.hooks/pre-load config)))

  (post-load [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/post-load test-plan)))

  (pre-load-test [testable config]
    (as-> testable $
      (reduce #(%2 %1 config) $ (:kaocha.hooks/pre-load-test config))
      (reduce #(%2 %1 config) $ (:kaocha.hooks/pre-load-test testable))))

  (post-load-test [testable config]
    (as-> testable $
      (reduce #(%2 %1 config) $ (:kaocha.hooks/post-load-test testable))
      (reduce #(%2 %1 config) $ (:kaocha.hooks/post-load-test config))))

  (pre-run [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/pre-run test-plan)))

  (post-run [test-plan]
    (reduce #(%2 %1) test-plan (:kaocha.hooks/post-run test-plan)))

  (wrap-run [run test-plan]
    (reduce #(%2 %1) run (:kaocha.hooks/wrap-run test-plan)))

  (pre-test [testable test-plan]
    (as-> testable $
          (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/pre-test test-plan))
          (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/pre-test testable))))

  (post-test [testable test-plan]
    (as-> testable $
          (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/post-test testable))
          (reduce #(%2 %1 test-plan) $ (:kaocha.hooks/post-test test-plan))))

  (pre-report [event]
    (reduce #(%2 %1) event (:kaocha.hooks/pre-report testable/*test-plan*)))

  (post-summary [test-result]
    (reduce #(%2 %1) test-result (:kaocha.hooks/post-summary test-result))))

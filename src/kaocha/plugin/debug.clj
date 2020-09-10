(ns kaocha.plugin.debug
  "Plugin that implements all possible plugin hooks and prints out a debug message
  with the hook name, and a subset of the first argument passed to the hook.

  For hooks that receive a testable it prints the testable id and type, for
  hooks that receive a test event it prints the type, file and line.

  Customize which values are printed with

  ``` clojure
  :kaocha/bindings {kaocha.plugin.debug/*keys* [,,,]}
  ```"
  (:require [kaocha.plugin :as plugin]))

(def id :kaocha.plugin/debug)

(def ^:dynamic *keys* [:kaocha.testable/id
                       :kaocha.testable/type
                       :type
                       :file
                       :line])

(defmethod plugin/-register id [_ plugins]
  (conj plugins
        (into {:kaocha.plugin/id id
               :kaocha.plugin/description "Show all hooks that are invoked"}
              (map (fn [hook]
                     [hook (fn [& args]
                             (print "[DEBUG]" hook "")
                             (println (cond-> (first args)
                                        (map? (first args))
                                        (select-keys *keys*)))
                             (first args))]))
              plugin/all-hooks)))

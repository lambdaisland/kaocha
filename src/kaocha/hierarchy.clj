(ns kaocha.hierarchy
  (:refer-clojure :exclude [isa?]))

(defonce hierarchy (make-hierarchy))

(defn derive!
  "Add a parent/child relationship to kaocha's keyword hierarchy."
  [tag parent]
  (alter-var-root #'hierarchy derive tag parent))

(derive! :fail :kaocha/fail-type)
(derive! :error :kaocha/fail-type)

(derive! :pass :kaocha/known-key)
(derive! :fail :kaocha/known-key)
(derive! :error :kaocha/known-key)
(derive! :begin-test-suite :kaocha/known-key)
(derive! :end-test-suite :kaocha/known-key)
(derive! :begin-test-ns :kaocha/known-key)
(derive! :end-test-ns :kaocha/known-key)
(derive! :begin-test-var :kaocha/known-key)
(derive! :end-test-var :kaocha/known-key)
(derive! :summary :kaocha/known-key)

(defn isa? [tag parent]
  (clojure.core/isa? hierarchy tag parent))

(defn fail-type? [event]
  (isa? (:type event) :kaocha/fail-type))

(defn known-key? [event]
  (isa? (:type event) :kaocha/known-key))

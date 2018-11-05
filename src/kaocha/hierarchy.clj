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
(derive! :begin-test-var :kaocha/known-key)
(derive! :end-test-var :kaocha/known-key)
(derive! :summary :kaocha/known-key)
(derive! :kaocha/pending :kaocha/known-key)

(derive! :begin-test-ns :kaocha/begin-group)
(derive! :end-test-ns :kaocha/end-group)

(derive! :kaocha/begin-group :kaocha/known-key)
(derive! :kaocha/end-group :kaocha/known-key)

(derive! :begin-test-var :kaocha/begin-test)
(derive! :end-test-var :kaocha/end-test)

(derive! :kaocha/begin-test :kaocha/known-key)
(derive! :kaocha/end-test :kaocha/known-key)

(derive! :kaocha/deferred :kaocha/known-key)

(defn isa? [tag parent]
  (clojure.core/isa? hierarchy tag parent))

(defn fail-type?
  "fail-type types indicate a failing test"
  [event]
  (isa? (:type event) :kaocha/fail-type))

(defn known-key?
  "Known keys don't get propogated to clojure.test/report, our own reporters
  already handle them."
  [event]
  (isa? (:type event) :kaocha/known-key))

(defn deferred?
  "Deferred events get propagated to clojure.test/report, but only during the
  summary step."
  [event]
  (isa? (:type event) :kaocha/deferred))

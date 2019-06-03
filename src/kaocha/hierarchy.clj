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

;; Legacy, prefer :kaocha/begin-suite and :kaocha/end-suite.
(derive! :begin-test-suite :kaocha/begin-suite)
(derive! :end-test-suite :kaocha/end-suite)

(derive! :begin-test-ns :kaocha/begin-group)
(derive! :end-test-ns :kaocha/end-group)

(derive! :kaocha/begin-group :kaocha/known-key)
(derive! :kaocha/end-group :kaocha/known-key)

;; Legacy, prefer :kaocha/begin-var and :kaocha/end-var.
(derive! :begin-test-var :kaocha/begin-test)
(derive! :end-test-var :kaocha/end-test)

(derive! :kaocha/begin-var :kaocha/begin-test)
(derive! :kaocha/end-var :kaocha/end-test)

(derive! :kaocha/begin-test :kaocha/known-key)
(derive! :kaocha/end-test :kaocha/known-key)

(derive! :kaocha/deferred :kaocha/known-key)

(defn isa? [tag parent]
  (or (clojure.core/isa? tag parent)
      (clojure.core/isa? hierarchy tag parent)))


;; Test event types

(defn fail-type?
  "Fail-type types indicate a failing test."
  [event]
  (isa? (:type event) :kaocha/fail-type))

(defn error-type?
  "Error-type indicates a test that failed because of an exception."
  [event]
  (isa? (:type event) :error))

(defn pass-type?
  "Error-type indicates a test that failed because of an exception."
  [event]
  (isa? (:type event) :pass))

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

(defn pending?
  "A test that generates a pending event will not be executed, but explicitly
  reported as being pending i.e. still needing to be implemented. Tests with
  the :kaocha/pending metadata will automatically generate a pending event."
  [event]
  (isa? (:type event) :kaocha/pending))

;; Testable types

(defn suite?
  "Top level testables are called suites, e.g. a suite of clojure.test tests."
  [testable]
  (isa? (:kaocha.testable/type testable) :kaocha.testable.type/suite))

(defn group?
  "Intermediary testables are called groups, e.g. a namespace of tests."
  [testable]
  (isa? (:kaocha.testable/type testable) :kaocha.testable.type/group))

(defn leaf?
  "This is a leaf in the tree of testables, i.e. it's an actual test with
  assertions, not just a container for tests.

  :kaocha.type/var is a leaf type, :kaocha.type/ns is not."
  [testable]
  (isa? (:kaocha.testable/type testable) :kaocha.testable.type/leaf))

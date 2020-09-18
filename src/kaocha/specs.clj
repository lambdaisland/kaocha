(ns kaocha.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [expound.alpha :as expound]))

(try
  (require 'clojure.test.check.generators)
  (catch java.io.FileNotFoundException _))

(def global-opts [:kaocha/reporter
                  :kaocha/color?
                  :kaocha/fail-fast?
                  :kaocha/watch?
                  :kaocha/plugins])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; global

(s/def :kaocha/plugins (s/coll-of keyword?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; config

(s/def :kaocha/config (s/keys :req ~(conj global-opts :kaocha/tests)))

(s/def :kaocha/tests (s/coll-of :kaocha/testable))

(s/def :kaocha/testable (s/keys :req [:kaocha.testable/type
                                      :kaocha.testable/id]
                                :opt [:kaocha.testable/meta
                                      :kaocha.testable/wrap]))

(s/def :kaocha.testable/meta (s/nilable map?))

(s/def :kaocha.testable/type qualified-keyword?)

(s/def :kaocha.testable/id keyword?)

;; Short description as used by the documentation reporter. No newlines.
(s/def :kaocha.testable/desc string?)

(s/def :kaocha.testable/wrap
  (s/with-gen
    (s/coll-of fn? :into [])
    #(s/gen #{[]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test plan

(s/def :kaocha/test-plan (s/keys :req ~(conj global-opts :kaocha.test-plan/tests)))

(s/def :kaocha.test-plan/tests (s/coll-of :kaocha.test-plan/testable))

(s/def :kaocha.test-plan/testable (s/merge :kaocha/testable
                                           (s/keys :req []
                                                   :opt [:kaocha.testable/desc
                                                         :kaocha.test-plan/tests
                                                         :kaacha.test-plan/load-error])))

(s/def :kaacha.test-plan/load-error (s/with-gen #(instance? Throwable %)
                                      #(s/gen #{(ex-info {:oops "not good"} "load error")})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; result

(s/def :kaocha/result (s/keys :req ~(conj global-opts :kaocha.result/tests)))

(s/def :kaocha.result/tests (s/coll-of :kaocha.result/testable))

(s/def :kaocha.result/testable (s/merge :kaocha.test-plan/testable
                                        (s/keys :opt [:kaocha.result/count
                                                      :kaocha.result/tests
                                                      :kaocha.result/pass
                                                      :kaocha.result/error
                                                      :kaocha.result/fail
                                                      :kaocha.result/out
                                                      :kaocha.result/err
                                                      :kaocha.result/time])))

(s/def ::small-int (s/with-gen nat-int?
                     (constantly (or (some-> (resolve `clojure.test.check.generators/small-integer) deref)
                                     (s/gen nat-int?)))))

(s/def :kaocha.result/count ::small-int)
(s/def :kaocha.result/pass ::small-int)
(s/def :kaocha.result/fail ::small-int)
(s/def :kaocha.result/pending ::small-int)
(s/def :kaocha.result/error ::small-int)

(s/def :kaocha.result/out string?)
(s/def :kaocha.result/err string?)

(s/def :kaocha.result/time nat-int?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clojure.spec.test

(when (find-ns 'clojure.spec.test.check)
  (s/def :clojure.spec.test.check/instrument? (s/nilable boolean?))
  (s/def :clojure.spec.test.check/check-asserts? (s/nilable boolean?))

  ;; TODO: Why is this not defined in core? Furthermore, I'm annoyed that the
  ;; implementation of clojure.spec.alpha.test does not follow spec's guideline of
  ;; using flat maps with namespaced keys. :clojure.spec.test.check/opts is a sub-map with
  ;; un-namespaced keys, and that's now propagating out into this library.
  (s/def :clojure.spec.test.check/num-tests (s/nilable nat-int?))
  (s/def :clojure.spec.test.check/max-size (s/nilable nat-int?))
  (s/def :clojure.spec.test.check/opts (s/nilable (s/keys :opt-un [:clojure.spec.test.check/num-tests
                                                                   :clojure.spec.test.check/max-size]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers

(defn assert-spec [spec value]
  (when-not (s/valid? spec value)
    (throw (AssertionError. (expound/expound-str spec value)))))

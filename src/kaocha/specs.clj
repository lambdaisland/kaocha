(ns kaocha.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [expound.alpha :as expound])
  (:import (java.io FileNotFoundException)))

(defn s-gen [_])
(defn s-with-gen [spec _] spec)
(defn s-fspec [_ __] any?)

(try
  (require 'clojure.test.check.generators)
  (def s-gen @(resolve 'clojure.spec.alpha/gen))
  (def s-with-gen @(resolve 'clojure.spec.alpha/with-gen))
  (defmacro s-fspec [& args] `(s/fspec ~@args))
  (catch FileNotFoundException _))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; global

(s/def :kaocha/color? boolean?)

(s/def :kaocha/fail-fast? boolean?)

(s/def :kaocha/watch? boolean?)

(s/def :kaocha/plugins (s/coll-of keyword?))

(s/def :kaocha/reporter (s/or :fn      (s-fspec :args (s/cat :m map?))
                              :symbol  symbol?
                              :symbols (s/coll-of symbol? :kind vector?)))

(s/def :kaocha/global-opts
  (s/keys :opt [:kaocha/reporter
                :kaocha/color?
                :kaocha/fail-fast?
                :kaocha/watch?
                :kaocha/plugins]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; config

(s/def :kaocha/config (s/merge :kaocha/global-opts
                               (s/keys :opt [:kaocha/tests])))

(s/def :kaocha/tests (s/coll-of :kaocha/testable))

(s/def :kaocha/testable (s/keys :req [:kaocha.testable/type
                                      :kaocha.testable/id]
                                :opt [:kaocha.testable/meta
                                      :kaocha.testable/wrap]))

(s/def :kaocha/source-paths (s/coll-of string?))

(s/def :kaocha/test-paths (s/coll-of string?))

(s/def :kaocha/ns-patterns (s/coll-of string?))

(s/def :kaocha.filter/skip-meta (s/coll-of keyword?))

(s/def :kaocha.testable/meta (s/nilable map?))

(s/def :kaocha.testable/type qualified-keyword?)

(s/def :kaocha.testable/id keyword?)

;; Short description as used by the documentation reporter. No newlines.
(s/def :kaocha.testable/desc string?)

(s/def :kaocha.testable/wrap
  (s-with-gen
   (s/coll-of fn? :into [])
   #(s-gen #{[]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test plan

(s/def :kaocha/test-plan
  (s/merge :kaocha/global-opts
           (s/keys :opt [:kaocha.test-plan/tests])))

(s/def :kaocha.test-plan/tests (s/coll-of :kaocha.test-plan/testable))

(s/def :kaocha.test-plan/testable (s/merge :kaocha/testable
                                           (s/keys :req []
                                                   :opt [:kaocha.testable/desc
                                                         :kaocha.test-plan/tests
                                                         :kaocha.test-plan/load-error])))

(s/def :kaocha.test-plan/load-error (s-with-gen
                                     #(instance? Throwable %)
                                     #(s-gen #{(ex-info {:oops "not good"} "load error")})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; result

(s/def :kaocha/result
  (s/merge :kaocha/global-opts
           (s/keys :opt [:kaocha.result/tests])))

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

(s/def ::small-int (s-with-gen
                    nat-int?
                    (constantly (or (some-> (resolve `clojure.test.check.generators/small-integer) deref)
                                    (s-gen nat-int?)))))

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

(try
  (require 'clojure.spec.test.alpha)
  (alias 'stc 'clojure.spec.test.check)
  (catch FileNotFoundException _))

(when (find-ns 'clojure.spec.test.check)
  (s/def ::stc/instrument? (s/nilable boolean?))
  (s/def ::stc/check-asserts? (s/nilable boolean?))

  ;; TODO: Why is this not defined in core? Furthermore, I'm annoyed that the
  ;; implementation of clojure.spec.alpha.test does not follow spec's guideline of
  ;; using flat maps with namespaced keys. :clojure.spec.test.check/opts is a sub-map with
  ;; un-namespaced keys, and that's now propagating out into this library.
  (s/def ::stc/num-tests (s/nilable nat-int?))
  (s/def ::stc/max-size (s/nilable nat-int?))
  (s/def ::stc/opts (s/nilable (s/keys :opt-un [::stc/num-tests
                                                ::stc/max-size]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers

(defn assert-spec [spec value]
  (when-not (s/valid? spec value)
    (throw (AssertionError. (expound/expound-str spec value)))))

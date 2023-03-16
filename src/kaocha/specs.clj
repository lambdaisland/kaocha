(ns kaocha.specs
  (:require [clojure.spec.alpha :as spec]
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
  (defmacro s-fspec [& args] `(spec/fspec ~@args))
  (catch FileNotFoundException _))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; global

(spec/def :kaocha/color? boolean?)

(spec/def :kaocha/fail-fast? boolean?)

(spec/def :kaocha/watch? boolean?)

(spec/def :kaocha/plugins (spec/coll-of keyword?))

(spec/def :kaocha/reporter (spec/or :fn      (s-fspec :args (spec/cat :m map?))
                                    :symbol  symbol?
                                    :symbols (spec/coll-of symbol? :kind vector?)))

(spec/def :kaocha/global-opts
  (spec/keys :opt [:kaocha/reporter
                   :kaocha/color?
                   :kaocha/fail-fast?
                   :kaocha/zero-assertion?
                   :kaocha/watch?
                   :kaocha/plugins]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; config

(spec/def :kaocha/config (spec/merge :kaocha/global-opts
                                     (spec/keys :opt [:kaocha/tests])))

(spec/def :kaocha/tests (spec/coll-of :kaocha/testable))

(spec/def :kaocha/testable (spec/keys :req [:kaocha.testable/type
                                            :kaocha.testable/id]
                                      :opt [:kaocha.testable/meta
                                            :kaocha.testable/wrap]))

(spec/def :kaocha/source-paths (spec/coll-of string?))

(spec/def :kaocha/test-paths (spec/coll-of string?))

(spec/def :kaocha/ns-patterns (spec/coll-of string?))

(spec/def :kaocha.filter/skip-meta (spec/coll-of keyword?))

(spec/def :kaocha.testable/meta (spec/nilable map?))

(spec/def :kaocha.testable/type qualified-keyword?)

(spec/def :kaocha.testable/id keyword?)

;; Short description as used by the documentation reporter. No newlines.
(spec/def :kaocha.testable/desc string?)

(spec/def :kaocha.testable/wrap
  (s-with-gen
   (spec/coll-of fn? :into [])
   #(s-gen #{[]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test plan

(spec/def :kaocha/test-plan
  (spec/merge :kaocha/global-opts
              (spec/keys :opt [:kaocha.test-plan/tests])))

(spec/def :kaocha.test-plan/tests (spec/coll-of :kaocha.test-plan/testable))

(spec/def :kaocha.test-plan/testable (spec/merge :kaocha/testable
                                                 (spec/keys :req []
                                                            :opt [:kaocha.testable/desc
                                                                  :kaocha.test-plan/tests
                                                                  :kaocha.testable/load-error])))

(spec/def :kaocha.testable/load-error (s-with-gen
                                       #(instance? Throwable %)
                                       #(s-gen #{(ex-info "load error" {:oops "not good"})})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; result

(spec/def :kaocha/result
  (spec/merge :kaocha/global-opts
              (spec/keys :opt [:kaocha.result/tests])))

(spec/def :kaocha.result/tests (spec/coll-of :kaocha.result/testable))

(spec/def :kaocha.result/testable (spec/merge :kaocha.test-plan/testable
                                              (spec/keys :opt [:kaocha.result/count
                                                               :kaocha.result/tests
                                                               :kaocha.result/pass
                                                               :kaocha.result/error
                                                               :kaocha.result/fail
                                                               :kaocha.result/out
                                                               :kaocha.result/err
                                                               :kaocha.result/time])))

(spec/def ::small-int (s-with-gen
                       nat-int?
                       (constantly (or (some-> (resolve `clojure.test.check.generatorspec/small-integer) deref)
                                       (s-gen nat-int?)))))

(spec/def :kaocha.result/count ::small-int)
(spec/def :kaocha.result/pass ::small-int)
(spec/def :kaocha.result/fail ::small-int)
(spec/def :kaocha.result/pending ::small-int)
(spec/def :kaocha.result/error ::small-int)

(spec/def :kaocha.result/out string?)
(spec/def :kaocha.result/err string?)

(spec/def :kaocha.result/time nat-int?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clojure.spec.test

(try
  (require 'clojure.spec.test.alpha)
  (alias 'stc 'clojure.spec.test.check)
  (catch FileNotFoundException _))

(when (find-ns 'clojure.spec.test.check)
  (spec/def ::stc/instrument? (spec/nilable boolean?))
  (spec/def ::stc/check-asserts? (spec/nilable boolean?))

  ;; TODO: Why is this not defined in core? Furthermore, I'm annoyed that the
  ;; implementation of clojure.spec.alpha.test does not follow spec's guideline of
  ;; using flat maps with namespaced keys. :clojure.spec.test.check/opts is a sub-map with
  ;; un-namespaced keys, and that's now propagating out into this library.
  (spec/def ::stc/num-tests (spec/nilable nat-int?))
  (spec/def ::stc/max-size (spec/nilable nat-int?))
  (spec/def ::stc/opts (spec/nilable (spec/keys :opt-un [::stc/num-tests
                                                         ::stc/max-size]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers

(defn assert-spec [spec value]
  (when-not (spec/valid? spec value)
    (throw (AssertionError. (expound/expound-str spec value)))))

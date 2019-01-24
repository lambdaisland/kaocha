(ns kaocha.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test :as t]
            [expound.alpha :as expound]))

(def global-opts [:kaocha/reporter
                  :kaocha/color?
                  :kaocha/fail-fast?
                  :kaocha/watch?
                  :kaocha/plugins])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; config

(s/def :kaocha/config (s/keys :req ~(conj global-opts :kaocha/tests)))

(s/def :kaocha/tests (s/coll-of :kaocha/testable))

(s/def :kaocha/testable (s/keys :req [:kaocha.testable/type
                                      :kaocha.testable/id]
                                :opt [:kaocha.testable/meta
                                      :kaocha.testable/wrap]))

(s/def :kaocha.testable/type qualified-keyword?)

(s/def :kaocha.testable/id keyword?)

;; Short description as used by the documentation reporter. No newlines.
(s/def :kaocha.testable/desc string?)

(s/def :kaocha.testable/wrap (s/coll-of fn? :into []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test plan

(s/def :kaocha/test-plan (s/keys :req ~(conj global-opts :kaocha.test-plan/tests)))

(s/def :kaocha.test-plan/tests (s/coll-of :kaocha.test-plan/testable))

(s/def :kaocha.test-plan/testable (s/and :kaocha/testable
                                         (s/keys :req []
                                                 :opt [:kaocha.testable/desc
                                                       :kaocha.test-plan/tests
                                                       :kaacha.test-plan/load-error])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; result

(s/def :kaocha/result (s/keys :req ~(conj global-opts :kaocha.result/tests)))

(s/def :kaocha.result/tests (s/coll-of :kaocha.result/testable))

(s/def :kaocha.result/testable (s/and :kaocha.test-plan/testable
                                      (s/keys :opt [:kaocha.result/count
                                                    :kaocha.result/tests
                                                    :kaocha.result/pass
                                                    :kaocha.result/error
                                                    :kaocha.result/fail
                                                    :kaocha.result/out
                                                    :kaocha.result/err
                                                    :kaocha.result/time])))

(s/def :kaocha.result/count nat-int?)
(s/def :kaocha.result/pass nat-int?)
(s/def :kaocha.result/fail nat-int?)
(s/def :kaocha.result/error nat-int?)

(s/def :kaocha.result/out string?)
(s/def :kaocha.result/err string?)

(s/def :kaocha.result/time nat-int?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers

(defn assert-spec [spec value]
  (when-not (s/valid? spec value)
    (throw (AssertionError. (expound/expound-str spec value)))))

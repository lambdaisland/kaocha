(ns kaocha.spec-test-check
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [kaocha.testable :as testable]
            [kaocha.type :as type]))

(alias 'stc 'clojure.spec.test.check)

(defn opt-key? [kw]
  (some-> kw (namespace) (str/starts-with? "clojure.spec.test.check")))

(defn opt-keys [m]
  (->> m (keys) (filter opt-key?)))

(defn opts [m]
  (->> m
       (opt-keys)
       (select-keys m)))

(def is-stc? (comp #{:kaocha.type/clojure.spec.test.check}
                :kaocha.testable/type))

(defn has-stc? [tests]
  (some is-stc? tests))


;; TODO: Why is this not defined in core?
(s/def ::stc/opts (s/keys :opt [::stc/num-tests
                                ::stc/max-size]))

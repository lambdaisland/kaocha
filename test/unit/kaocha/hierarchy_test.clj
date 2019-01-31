(ns kaocha.hierarchy-test
  (:require [clojure.test :refer :all]
            [kaocha.hierarchy :as h]))

(require 'kaocha.type.clojure.test)

(derive ::global-leaf :kaocha.testable.type/leaf)
(derive ::global-group :kaocha.testable.type/group)
(derive ::global-suite :kaocha.testable.type/suite)
(h/derive! ::local-leaf :kaocha.testable.type/leaf)
(h/derive! ::local-group :kaocha.testable.type/group)
(h/derive! ::local-suite :kaocha.testable.type/suite)

(deftest fail-type?-test
  (is (h/fail-type? {:type :fail}))
  (is (h/fail-type? {:type :error})))

(deftest error-type?-test
  (is (h/error-type? {:type :error})))

(deftest pass-type?-test
  (is (h/pass-type? {:type :pass})))

(deftest known-key?-test
  (is (h/known-key? {:type :pass}))
  (is (h/known-key? {:type :fail}))
  (is (h/known-key? {:type :error}))
  (is (h/known-key? {:type :kaocha/known-key}))
  (is (h/known-key? {:type :kaocha/deferred}))
  (is (not (h/known-key? {:type :kaocha/foo}))))

(derive ::global-deferred :kaocha/deferred)
(h/derive! ::local-deferred :kaocha/deferred)

(deftest deferred?-test
  (is (h/deferred? {:type ::global-deferred}))
  (is (h/deferred? {:type ::local-deferred})))

(deftest pending?-test
  (is (h/pending? {:type :kaocha/pending})))

(deftest suite-test
  (is (h/suite? {:kaocha.testable/type :kaocha.testable.type/suite}))
  (is (h/suite? {:kaocha.testable/type :kaocha.type/clojure.test}))
  (is (h/suite? {:kaocha.testable/type ::global-suite}))
  (is (h/suite? {:kaocha.testable/type ::local-suite})))

(deftest group-test
  (is (h/group? {:kaocha.testable/type :kaocha.testable.type/group}))
  (is (h/group? {:kaocha.testable/type :kaocha.type/ns}))
  (is (h/group? {:kaocha.testable/type ::global-group}))
  (is (h/group? {:kaocha.testable/type ::local-group})))

(deftest leaf-test
  (is (h/leaf? {:kaocha.testable/type :kaocha.testable.type/leaf}))
  (is (h/leaf? {:kaocha.testable/type :kaocha.type/var}))
  (is (h/leaf? {:kaocha.testable/type ::global-leaf}))
  (is (h/leaf? {:kaocha.testable/type ::local-leaf})))

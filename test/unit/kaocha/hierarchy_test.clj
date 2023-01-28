(ns kaocha.hierarchy-test
  (:require [clojure.test :refer :all]
            [kaocha.hierarchy :as hierarchy]))

(require 'kaocha.type.clojure.test)

(derive ::global-leaf :kaocha.testable.type/leaf)
(derive ::global-group :kaocha.testable.type/group)
(derive ::global-suite :kaocha.testable.type/suite)
(hierarchy/derive! ::local-leaf :kaocha.testable.type/leaf)
(hierarchy/derive! ::local-group :kaocha.testable.type/group)
(hierarchy/derive! ::local-suite :kaocha.testable.type/suite)

(deftest fail-type?-test
  (is (hierarchy/fail-type? {:type :fail}))
  (is (hierarchy/fail-type? {:type :error})))

(deftest error-type?-test
  (is (hierarchy/error-type? {:type :error})))

(deftest pass-type?-test
  (is (hierarchy/pass-type? {:type :pass})))

(deftest known-key?-test
  (is (hierarchy/known-key? {:type :pass}))
  (is (hierarchy/known-key? {:type :fail}))
  (is (hierarchy/known-key? {:type :error}))
  (is (hierarchy/known-key? {:type :kaocha/known-key}))
  (is (hierarchy/known-key? {:type :kaocha/deferred}))
  (is (not (hierarchy/known-key? {:type :kaocha/foo}))))

(derive ::global-deferred :kaocha/deferred)
(hierarchy/derive! ::local-deferred :kaocha/deferred)

(deftest deferred?-test
  (is (hierarchy/deferred? {:type ::global-deferred}))
  (is (hierarchy/deferred? {:type ::local-deferred})))

(deftest pending?-test
  (is (hierarchy/pending? {:type :kaocha/pending})))

(deftest suite-test
  (is (hierarchy/suite? {:kaocha.testable/type :kaocha.testable.type/suite}))
  (is (hierarchy/suite? {:kaocha.testable/type :kaocha.type/clojure.test}))
  (is (hierarchy/suite? {:kaocha.testable/type ::global-suite}))
  (is (hierarchy/suite? {:kaocha.testable/type ::local-suite})))

(deftest group-test
  (is (hierarchy/group? {:kaocha.testable/type :kaocha.testable.type/group}))
  (is (hierarchy/group? {:kaocha.testable/type :kaocha.type/ns}))
  (is (hierarchy/group? {:kaocha.testable/type ::global-group}))
  (is (hierarchy/group? {:kaocha.testable/type ::local-group})))

(deftest leaf-test
  (is (hierarchy/leaf? {:kaocha.testable/type :kaocha.testable.type/leaf}))
  (is (hierarchy/leaf? {:kaocha.testable/type :kaocha.type/var}))
  (is (hierarchy/leaf? {:kaocha.testable/type ::global-leaf}))
  (is (hierarchy/leaf? {:kaocha.testable/type ::local-leaf})))

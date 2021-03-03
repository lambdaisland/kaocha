(ns kaocha.version-check-test
  (:require [clojure.test :refer :all]
            [kaocha.test-helper]
            [kaocha.version-check :as v]))

(deftest earlier-version-throws
  (is (thrown-ex-data?
       "Kaocha requires Clojure 1.9 or later."
       {:kaocha/early-exit 251}
       (binding [*clojure-version* {:major 1 :minor 8}]
         (v/check-version-minimum 1 9)))))

(deftest current-version-does-not-throw
  (is (nil? (binding [*clojure-version* {:major 1 :minor 9}]
        (v/check-version-minimum 1 9)))))


(deftest version-2-does-not-throw
  (is (nil? (binding [*clojure-version* {:major 2 :minor 0}]
        (v/check-version-minimum 1 9)))))


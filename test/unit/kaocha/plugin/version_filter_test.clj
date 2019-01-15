(ns kaocha.plugin.version-filter-test
  (:require [clojure.test :refer [is]]
            [kaocha.test :refer [deftest]]
            [kaocha.plugin.version-filter :as v]))

(defmacro with-java-version
  {:style/indent [1]}
  [version & body]
  `(let [original# (System/getProperty "java.runtime.version")]
     (try
       (System/setProperty "java.runtime.version" ~version)
       ~@body
       (finally
         (System/setProperty "java.runtime.version" original#)))))

(defmacro with-clojure-version
  {:style/indent [1]}
  [version & body]
  `(binding [*clojure-version* (zipmap [:major :minor :incremental] (v/version-vector ~version))]
     ~@body))

(deftest version-vector-test
  (is (= (v/version-vector "1.10.0") [1 10 0]))
  (is (= (v/version-vector "1.10.0-beta5") [1 10 0]))
  (is (= (v/version-vector "10.0.2+13-Ubuntu-1ubuntu0.18.04.4") [10 0 2])))

(deftest compare-versions-test
  (is (= 0 (v/compare-versions "1.10.0" "1.10.0")))
  (is (= 0 (v/compare-versions "1.10.0" "1.10")))
  (is (= 0 (v/compare-versions "1.10" "1.10.0")))
  (is (= -1 (v/compare-versions "1.9" "1.10")))
  (is (= 1 (v/compare-versions "1.10" "1.9")))
  (is (= 0 (v/compare-versions "1.10" "1.10.0")))
  (is (= 1 (v/compare-versions "1.10.1" "1.10.0"))))

(deftest version>=?-test
  (is (v/version>=? "1.10.0" "1.9"))
  (is (v/version>=? "1.10.0" "1.10"))
  (is (not (v/version>=? "1.10.0" "1.10.1")))
  (is (not (v/version>=? "1.9" "1.10.0")))
  (is (v/version>=? "1.10" "1.10.0"))
  (is (v/version>=? "1.10.1" "1.10.0"))
  (is (v/version>=? "1" nil))
  (is (v/version>=? nil "1"))
  (is (v/version>=? nil nil)))

(deftest skip?-test
  (with-clojure-version "1.10.0"
    (with-java-version "9"
      (is (not (v/skip? {:kaocha.testable/meta {:min-clojure-version "1.10"
                                                :max-java-version "10"}})))))

  (with-clojure-version "1.9.0"
    (with-java-version "9"
      (is (v/skip? {:kaocha.testable/meta {:min-clojure-version "1.10"
                                           :max-java-version "10"}}))))

  (with-clojure-version "1.10.0"
    (with-java-version "11"
      (is (v/skip? {:kaocha.testable/meta {:min-clojure-version "1.10"
                                           :max-java-version "10"}})))))

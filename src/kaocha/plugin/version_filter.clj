(ns kaocha.plugin.version-filter
  "Filter tests based on the Clojure or Java version.

  This plugin will look for test metadata specifying the minimum or maximum
  version of Clojure or Java this test is designed to work with.

  The recognized metadata keys are `:min-clojure-version`,
  `:max-clojure-version`, `:min-java-version`, and `:max-java-version`. The
  associated value is a version string, such as `\"1.10.0\"`.

  You can set both a minimum and a maximum to limit to a certain range. The
  boundaries are always inclusive, so `^{:max-clojure-version \"1.9\"}` will run
  on Clojure `1.9.*` or earlier.

  Specificty matters, a test with a max version of `\"1.10\" will also run on
  version `\"1.10.2\"`, whereas if the max version is `\"1.10.0\"` it will not."
  (:require [clojure.string :as str]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]))

(defn version-vector [v]
  (let [v (first (str/split v #"\+"))]
    ;; if the segment starts with digits then parse those and compare them
    ;; numerically, else keep the segment and compare it as a string.
    (mapv #(if-let [num (re-find #"^\d+" %)]
             (Integer/parseInt num)
             %)
          (clojure.string/split v #"\."))))

(defn java-version []
  (System/getProperty "java.runtime.version"))

(defn compare-versions [v1 v2]
  (let [v1 (version-vector v1)
        v2 (version-vector v2)
        significance (min (count v1) (count v2))]
    (compare (vec (take significance v1))
             (vec (take significance v2)))))

(defn version>=? [v1 v2]
  (if (and v1 v2)
    (>= (compare-versions v1 v2) 0)
    true))

(defn skip? [testable]
  (let [{:keys [min-clojure-version
                max-clojure-version
                min-java-version
                max-java-version]}
        (::testable/meta testable)]
    (not
     (and
      (version>=? (clojure-version) min-clojure-version)
      (version>=? max-clojure-version (clojure-version))
      (version>=? (java-version) min-java-version)
      (version>=? max-java-version (java-version))))))

(defplugin kaocha.plugin/version-filter
  (pre-test [testable test-plan]
    (if (skip? testable)
      (assoc testable ::testable/skip true)
      testable)))

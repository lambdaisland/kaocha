(ns lambdaisland.kaocha.config-test
  (:require [lambdaisland.kaocha.config :as config]
            [clojure.test :refer :all]))

(def default-config @#'config/default-config)

(deftest default-config-test
  (is (contains? (default-config) :suites)))

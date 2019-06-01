(ns kaocha.ns
  (:refer-clojure :exclude [symbol])
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test :as t]
            [kaocha.core-ext :refer :all]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [kaocha.type :as type]))

(defn required-ns [ns-name]
  (when-not (find-ns ns-name)
    (require ns-name))
  (the-ns ns-name))

(s/def :kaocha.ns/name simple-symbol?)
(s/def :kaocha.ns/ns   ns?)

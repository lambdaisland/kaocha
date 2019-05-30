(ns kaocha.load
  (:refer-clojure :exclude [symbol])
  (:require [kaocha.core-ext :refer :all]
            [kaocha.classpath :as classpath]
            [kaocha.testable :as testable]
            [clojure.java.io :as io]
            [lambdaisland.tools.namespace.find :as ctn-find]
            [kaocha.output :as output]))

(def clj ctn-find/clj)
(def cljs ctn-find/cljs)

(defn ns-match? [ns-patterns ns-sym]
  (some #(re-find % (name ns-sym)) ns-patterns))

(defn find-test-nss [test-paths ns-patterns & [platform]]
  (sequence (comp
             (map io/file)
             (map #(ctn-find/find-namespaces-in-dir % platform))
             cat
             (filter (partial ns-match? ns-patterns)))
            test-paths))

(defn namespace-testables [paths-key testable ns-testable-fn & [platform]]
  (let [test-paths  (paths-key testable)
        ns-patterns (map regex (:kaocha/ns-patterns testable))
        ns-names    (find-test-nss test-paths ns-patterns platform)
        testables   (map ns-testable-fn ns-names)]
    (testable/load-testables testables)))

(defn load-namespaces [paths-key testable ns-testable-fn & [platform]]
  (assoc testable :kaocha.test-plan/tests
         (namespace-testables paths-key testable ns-testable-fn platform)))

(def load-test-namespaces   (partial load-namespaces :kaocha/test-paths))

(ns kaocha.type.suite
  (:require [kaocha.core-ext :refer :all]
            [clojure.spec.alpha :as s]
            [kaocha.type.ns :as type.ns]
            [kaocha.testable :as testable]
            [kaocha.classpath :as classpath]
            [clojure.tools.namespace.find :as ctn.find]
            [clojure.java.io :as io]
            [clojure.test :as t]))

(defn- ns-match? [ns-patterns ns-sym]
  (some #(re-find % (name ns-sym)) ns-patterns))

(defn- find-test-nss [test-paths ns-patterns]
  (sequence (comp
             (map io/file)
             (map ctn.find/find-namespaces-in-dir)
             cat
             (filter (partial ns-match? ns-patterns)))
            test-paths))

(defmethod testable/-load :kaocha.type/suite [testable]
  (let [{:kaocha.suite/keys [test-paths ns-patterns]} testable
        ns-patterns                                   (map regex ns-patterns)]
    (classpath/maybe-add-dynamic-classloader)
    (run! classpath/add-classpath test-paths)
    (let [ns-names  (find-test-nss test-paths ns-patterns)
          testables (map type.ns/->testable ns-names)]
      (assoc testable :kaocha.test-plan/tests
             (doall (map testable/load testables))))))

(defmethod testable/-run :kaocha.type/suite [testable]
  (t/do-report (assoc testable :type :begin-test-suite))
  (let [results (testable/run-testables (:kaocha.test-plan/tests testable))
        testable (-> testable
                     (dissoc :kaocha.test-plan/tests)
                     (assoc :kaocha.result/tests results))]
    (t/do-report (assoc testable :type :end-test-suite))
    testable))

(s/def :kaocha.type/suite (s/keys :req [:kaocha.suite/source-paths
                                        :kaocha.suite/test-paths
                                        :kaocha.suite/ns-patterns]))

(s/def :kaocha.suite/source-paths (s/coll-of string?))
(s/def :kaocha.suite/test-paths (s/coll-of string?))
(s/def :kaocha.suite/ns-patterns (s/coll-of string?))

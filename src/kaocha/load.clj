(ns kaocha.load
  (:refer-clojure :exclude [symbol])
  (:require [kaocha.core-ext :refer :all]
            [kaocha.testable :as testable]
            [clojure.java.io :as io]
            [lambdaisland.tools.namespace.find :as ctn-find]))

(set! *warn-on-reflection* true)

(def clj ctn-find/clj)
(def cljs ctn-find/cljs)

(defn ns-match? [ns-patterns ns-sym-or-error]
  (or (ctn-find/reader-exception? ns-sym-or-error)
      (some #(re-find % (name ns-sym-or-error)) ns-patterns)))

(defn find-test-nss [test-paths ns-patterns & [platform]]
  (sequence (comp
             (map io/file)
             (mapcat #(ctn-find/find-namespaces-in-dir % platform))
             (filter (partial ns-match? ns-patterns)))
            test-paths))

(defn load-error-testable [file exception]
  {::testable/type               :kaocha.type/ns
   ::testable/id                 (keyword (str file))
   ::testable/desc               (str "ns form could not be read in " file)
   ::testable/load-error         exception
   ::testable/load-error-file    (str file)
   ::testable/load-error-line    1
   ::testable/load-error-message (str "Failed reading ns form in " file "\n"
                                      "Caused by: " (.getMessage ^Throwable exception))
   :kaocha.ns/name               'kaocha.load-error})

(defn load-test-namespaces [testable ns-testable-fn & [platform]]
  (let [test-paths  (:kaocha/test-paths testable)
        ns-patterns (map regex (:kaocha/ns-patterns testable))
        ns-names    (find-test-nss test-paths ns-patterns platform)
        testables   (map (fn [sym-or-error]
                           (if (ctn-find/reader-exception? sym-or-error)
                             (let [[_ file exception] sym-or-error]
                               (load-error-testable file exception))
                             (ns-testable-fn sym-or-error)))
                         ns-names)]
    (assoc testable
           :kaocha.test-plan/tests
           (testable/load-testables testables))))

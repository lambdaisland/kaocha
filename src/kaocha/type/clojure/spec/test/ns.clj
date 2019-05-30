(ns kaocha.type.clojure.spec.test.ns
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [kaocha.core-ext :as core]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]
            [kaocha.type.ns :as type.ns]))

(def ->testable (partial type.ns/testable :kaocha.type/ns))

(defmethod testable/-load :kaocha.type/ns-spec-fdefs [testable]
  (let [ns-name (-> testable :kaocha.ns/name type.ns/required-ns testable)
        ns-obj  (the-ns ns-name)]
    (->> (stest/checkable-syms)
         (filter (partial core/in-namespace? ns-name))
         (type.fdef/load-testables)
         (assoc testable
                :kaocha.testable/meta (meta ns-obj)
                :kaocha.ns/ns ns-obj
                :kaocha.test-plan/tests))))

(defmethod testable/-run :kaocha.type/ns-spec-fdefs [testable test-plan]
  (type.ns/run-testable testable test-plan))

(s/def :kaocha.type/ns-spec-fdefs :kaocha.type/ns)

(hierarchy/derive! :kaocha.type/ns-spec-fdefs :kaocha.testable.type/group)

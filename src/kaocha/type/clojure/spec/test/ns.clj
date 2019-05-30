(ns kaocha.type.clojure.spec.test.ns
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.ns :as ns]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]))

(defn ->testable [ns-name]
  {:kaocha.testable/type :kaocha.type/clojure.spec.test.ns
   :kaocha.testable/id   (keyword (str ns-name))
   :kaocha.testable/desc (str ns-name)
   :kaocha.ns/name       ns-name})

(defmethod testable/-load :kaocha.type/clojure.spec.test.ns [testable]
  (let [ns-name (-> testable :kaocha.ns/name ns/required-ns testable)
        ns-obj  (the-ns ns-name)]
    (->> (stest/checkable-syms)
         (filter (partial ns/starts-with-namespace? ns-name))
         (type.fdef/load-testables)
         (assoc testable
                :kaocha.testable/meta (meta ns-obj)
                :kaocha.ns/ns ns-obj
                :kaocha.test-plan/tests))))

(defmethod testable/-run :kaocha.type/clojure.spec.test.ns [testable test-plan]
  (ns/run-testable testable test-plan))

(s/def :kaocha.type/clojure.spec.test.ns (s/keys :req [:kaocha.testable/type
                                                       :kaocha.testable/id
                                                       :kaocha.ns/name]
                                                 :opt [:kaocha.ns/ns
                                                       :kaocha.test-plan/tests]))

(hierarchy/derive! :kaocha.type/clojure.spec.test.ns :kaocha.testable.type/group)

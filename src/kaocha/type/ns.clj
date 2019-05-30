(ns kaocha.type.ns
  (:refer-clojure :exclude [symbol])
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [kaocha.core-ext :refer :all]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.ns :as ns]
            [kaocha.testable :as testable]
            [kaocha.type :as type]))

(defn ->testable [ns-name]
  {:kaocha.testable/type :kaocha.type/ns
   :kaocha.testable/id   (keyword (str ns-name))
   :kaocha.testable/desc (str ns-name)
   :kaocha.ns/name       ns-name})

(defmethod testable/-load :kaocha.type/ns [testable]
  ;; TODO If the namespace has a test-ns-hook function, call that:
  ;; if-let [v (find-var (symbol (:kaocha.ns/name testable) "test-ns-hook"))]

  (let [ns-name         (-> testable :kaocha.ns/name ns/required-ns testable)
        ns-obj          (the-ns ns-name)
        ns-meta         (meta ns-obj)
        each-fixture-fn (t/join-fixtures (::t/each-fixtures ns-meta))]
    (->> ns-obj
         ns-publics
         (filter (comp :test meta val))
         (sort-by key)
         (map (fn [[sym var]]
                (let [nsname    (:kaocha.ns/name testable)
                      test-name (symbol (str nsname) (str sym))]
                  {:kaocha.testable/type :kaocha.type/var
                   :kaocha.testable/id   (keyword test-name)
                   :kaocha.testable/meta (meta var)
                   :kaocha.testable/desc (str sym)
                   :kaocha.var/name      test-name
                   :kaocha.var/var       var
                   :kaocha.var/test      (:test (meta var))
                   :kaocha.testable/wrap (if (::t/each-fixtures ns-meta)
                                           [(fn [t] #(each-fixture-fn t))]
                                           [])})))
         (assoc testable
                :kaocha.testable/meta (meta ns-obj)
                :kaocha.ns/ns ns-obj
                :kaocha.test-plan/tests))))

(defmethod testable/-run :kaocha.type/ns [testable test-plan]
  (ns/run-testable testable test-plan))

(s/def :kaocha.type/ns (s/keys :req [:kaocha.testable/type
                                     :kaocha.testable/id
                                     :kaocha.ns/name]
                               :opt [:kaocha.ns/ns
                                     :kaocha.test-plan/tests]))

(hierarchy/derive! :kaocha.type/ns :kaocha.testable.type/group)

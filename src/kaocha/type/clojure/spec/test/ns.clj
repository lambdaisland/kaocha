(ns kaocha.type.clojure.spec.test.ns
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.test.alpha]
            [clojure.test :as t]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.ns :as ns]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(defn ->testable [check ns-name]
  (->> {:kaocha.testable/type :kaocha.type/clojure.spec.test.ns
        :kaocha.testable/id   (keyword (str ns-name))
        :kaocha.testable/desc (str ns-name)
        :kaocha.ns/name       ns-name}
       (merge check)))

(defn starts-with-namespace? [ns-name sym-or-kw]
  (-> sym-or-kw namespace (= (str ns-name))))

(defmethod testable/-load :kaocha.type/clojure.spec.test.ns [testable]
  (let [ns-name (:kaocha.ns/name testable)
        ns-obj  (ns/required-ns ns-name)]
    (->> (stest/checkable-syms)
         (filter (partial starts-with-namespace? ns-name))
         (type.fdef/load-testables)
         (assoc testable
           :kaocha.testable/meta (meta ns-obj)
           :kaocha.ns/ns ns-obj
           :kaocha.test-plan/tests))))

(defmethod testable/-run :kaocha.type/clojure.spec.test.ns [testable test-plan]
  (let [do-report #(t/do-report (merge {:ns (:kaocha.ns/ns testable)} %))]
    (type/with-report-counters
      (do-report {:type :kaocha.stc/begin-ns})
      (if-let [testable (testable/handle-load-error testable)]
        (do
          (do-report {:type :kaocha.stc/end-ns})
          testable)
        (let [tests  (testable/run-testables (:kaocha.test-plan/tests testable) test-plan)
              result (-> testable
                         (dissoc :kaocha.test-plan/tests)
                         (assoc :kaocha.result/tests tests))]
          (do-report {:type :kaocha.stc/end-ns})
          result)))))

(s/def :kaocha.type/clojure.spec.test.ns (s/keys :req [:kaocha.testable/type
                                                       :kaocha.testable/id
                                                       :kaocha.ns/name]
                                                 :opt [:kaocha.ns/ns
                                                       :kaocha.test-plan/tests
                                                       ::stc/opts]))

(hierarchy/derive! :kaocha.type/clojure.spec.test.ns :kaocha.testable.type/group)
(hierarchy/derive! :kaocha.stc/begin-ns :kaocha/begin-group)
(hierarchy/derive! :kaocha.stc/end-ns :kaocha/end-group)

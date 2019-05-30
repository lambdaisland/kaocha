(ns kaocha.type.clojure.spec.test.check
  (:require [clojure.spec.alpha :as s]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.load :as load]
            [kaocha.spec-test-check :as k-stc]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]
            [kaocha.type.clojure.spec.test.ns :as type.spec.ns]))

(alias 'stc 'clojure.spec.test.check)

(defn check-tests [{::keys [syms] :as check}]
  (-> (condp = syms
        :all-fdefs   (load/namespace-testables :kaocha/source-paths check
                                               type.spec.ns/->testable)
        :other-fdefs nil ;; TODO: this requires orchestration from the plugin
        :else        (type.fdef/load-testables syms))
      (testable/add-desc "clojure.spec.test.check")))

(defn checks [{::keys [checks] :as testable}]
  (let [checks (or checks [{}])]
    (map #(merge testable %) checks)))

(defmethod testable/-load :kaocha.type/clojure.spec.test.check [testable]
  (->> (checks testable)
       (check-tests)
       (apply conj)
       (assoc testable :kaocha/tests)))

(s/def ::syms (s/or :given-symbols (s/coll-of symbol?)
                    :all #{:all-fdefs}
                    :rest #{:other-fdefs}))
(s/def ::check (s/merge (s/keys :opt [::syms]) ::stc/opts))
(s/def ::checks (s/coll-of ::check))

(s/def :kaocha.type/clojure.spec.test.check
  (s/merge (s/keys :req [:kaocha.testable/type
                         :kaocha.testable/id
                         :kaocha/source-paths]
                   :opt [:kaocha.filter/skip-meta
                         ::checks])
           ::check))

(hierarchy/derive! :kaocha.type/clojure.spec.test.check
                   :kaocha.testable.type/suite)
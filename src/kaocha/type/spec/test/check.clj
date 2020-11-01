(ns kaocha.type.spec.test.check
  (:refer-clojure :exclude [symbol])
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [kaocha.core-ext :refer :all]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.load :as load]
            [kaocha.specs]
            [kaocha.test-suite :as test-suite]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.spec.test.fdef :as type.fdef]
            [kaocha.type.spec.test.ns :as type.spec.ns]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(def check-defaults {:kaocha.spec.test.check/ns-patterns [".*"]
                     :kaocha.spec.test.check/syms :all-fdefs})

(defn all-fdef-tests [{:kaocha/keys [source-paths]
                       :kaocha.spec.test.check/keys [ns-patterns]
                       :as          testable}]
  (let [ns-patterns (map regex ns-patterns)
        ns-names    (load/find-test-nss source-paths ns-patterns)
        testables   (map #(type.spec.ns/->testable testable %) ns-names)]
    (testable/load-testables testables)))

(defn check-tests [check]
  (let [{syms :kaocha.spec.test.check/syms :as check} (merge check-defaults check)]
    (condp = syms
      :all-fdefs   (all-fdef-tests check)
      :other-fdefs nil ;; TODO: this requires orchestration from the plugin
      :else        (type.fdef/load-testables check syms))))

(defn checks [{checks :kaocha.spec.test.check/checks :as testable}]
  (let [checks (or checks [{}])]
    (map #(merge testable %) checks)))

(defmethod testable/-load :kaocha.type/spec.test.check [testable]
  (-> (checks testable)
      (->> (map check-tests)
           (apply concat)
           (assoc testable :kaocha.test-plan/tests))
      (testable/add-desc "clojure.spec.test.check")))

(defmethod testable/-run :kaocha.type/spec.test.check [testable test-plan]
  (test-suite/run testable test-plan))

(s/def :kaocha.spec.test.check/syms
  (s/or :given-symbols (s/coll-of symbol?)
        :catch-all #{:all-fdefs :other-fdefs}))

(s/def :kaocha.spec.test.check/ns-patterns :kaocha/ns-patterns)

(s/def :kaocha.spec.test.check/check
  (s/keys :opt [:kaocha.spec.test.check/syms
                ::stc/instrument?
                ::stc/check-asserts?
                ::stc/opts
                :kaocha.spec.test.check/ns-patterns]))

(s/def :kaocha.spec.test.check/checks (s/coll-of :kaocha.spec.test.check/check))

(s/def :kaocha.type/spec.test.check
  (s/merge (s/keys :req [:kaocha.testable/type
                         :kaocha.testable/id
                         :kaocha/source-paths]
                   :opt [:kaocha.filter/skip-meta
                         :kaocha.spec.test.check/ns-patterns
                         :kaocha.spec.test.check/checks])
           :kaocha.spec.test.check/check))

(hierarchy/derive! :kaocha.type/spec.test.check
                   :kaocha.testable.type/suite)

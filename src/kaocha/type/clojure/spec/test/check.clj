(ns kaocha.type.clojure.spec.test.check
  (:refer-clojure :exclude [symbol])
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha]
            [kaocha.core-ext :refer :all]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.load :as load]
            [kaocha.test-suite :as test-suite]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.test.fdef :as type.fdef]
            [kaocha.type.clojure.spec.test.ns :as type.spec.ns]))

;; This namespace does not actually exist, but is created by
;; requiring clojure.spec.test.alpha
(alias 'stc 'clojure.spec.test.check)

(defn all-fdef-tests [{:kaocha/keys [source-paths ns-patterns]
                       :or          {ns-pattens ["*"]}
                       :as          testable}]
  (let [ns-patterns (map regex ns-patterns)
        ns-names    (load/find-test-nss source-paths ns-patterns)
        testables   (map #(type.spec.ns/->testable testable %) ns-names)]
    (testable/load-testables testables)))

(defn check-tests [{::keys [syms] :as check}]
  (let [check (update check :kaocha/ns-patterns #(or % [".*"]))]
    (condp = syms
      :all-fdefs   (all-fdef-tests check)
      :other-fdefs nil ;; TODO: this requires orchestration from the plugin
      :else        (type.fdef/load-testables syms))))

(defn checks [{::keys [checks] :as testable}]
  (let [checks (or checks [{}])]
    (map #(merge testable %) checks)))

(defmethod testable/-load :kaocha.type/clojure.spec.test.check [testable]
  (-> (checks testable)
      (->> (map check-tests)
           (apply concat)
           (assoc testable :kaocha.test-plan/tests))
      (testable/add-desc "clojure.spec.test.check")))

(defmethod testable/-run :kaocha.type/clojure.spec.test.check [testable test-plan]
  (test-suite/run testable test-plan))

(s/def ::syms (s/or :given-symbols (s/coll-of symbol?)
                    :catch-all #{:all-fdefs :other-fdefs}))
(s/def ::check (s/keys :opt [::syms
                             ::stc/instrument?
                             ::stc/check-asserts?
                             ::stc/opts
                             :kaocha/ns-patterns]))
(s/def ::checks (s/coll-of ::check))

(s/def :kaocha.type/clojure.spec.test.check
  (s/merge (s/keys :req [:kaocha.testable/type
                         :kaocha.testable/id
                         :kaocha/source-paths]
                   :opt [:kaocha.filter/skip-meta
                         :kaocha/ns-patterns
                         ::checks])
           ::check))

(hierarchy/derive! :kaocha.type/clojure.spec.test.check
                   :kaocha.testable.type/suite)

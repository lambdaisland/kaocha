(ns kaocha.type.clojure.spec.alpha.check
  (:require [clojure.spec.alpha :as s]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.load :as load]
            [kaocha.testable :as testable]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.alpha.fdef :as type.fdef]
            [kaocha.type.clojure.spec.alpha.ns :as type.spec.ns]))

(alias 'stc 'clojure.spec.test.check)

(defmethod testable/-load :kaocha.type/clojure.spec.test.alpha.check
  [{::keys [syms] :as testable}]
  (-> (if (= syms :all-fdefs)
        (load/load-namespaces testable :kaocha/source-paths type.spec.ns/->testable)
        (assoc testable :kaocha.test-plan/tests (type.fdef/load-testables syms)))
      (testable/add-desc "clojure.spec.alpha.check")))

(s/def ::syms (s/or :given-symbols (s/coll-of symbol?)
                    :all #{:all-fdefs}))
(s/def ::checks (s/merge (s/keys :req [::syms]) ::stc/opts))

(s/def :kaocha.type/clojure.spec.test.alpha.check
  (s/keys :req [:kaocha.testable/type
                :kaocha.testable/id
                :kaocha/ns-patterns
                :kaocha/source-paths
                ::checks]
          :opt [:kaocha.filter/skip-meta]))

(hierarchy/derive! :kaocha.type/clojure.spec.test.alpha.check
                   :kaocha.testable.type/suite)

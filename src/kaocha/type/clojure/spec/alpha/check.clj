(ns kaocha.type.clojure.spec.alpha.check
  (:require [clojure.spec.alpha :as s]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.testable :as testable]
            [kaocha.type.clojure.spec.alpha.ns :as type.spec.ns]
            [kaocha.type.clojure.test :as clj-test]
            [kaocha.type :as type]))

(defmethod testable/-load :kaocha.type/clojure.spec.test.alpha.check
  [{::keys [syms] :as testable}]
  (-> (if (= syms :all-fdefs)
        (load/load-namespaces testable :kaocha/source-paths type.spec.ns/->testable)
        (type.spec.ns/load-fdefs testable))
      (testable/add-desc "clojure.spec.alpha.check")))

(s/def ::syms (s/or :given-symbols (s/coll-of (symbol?))
                    :all #{:all-fdefs}))
(s/def ::checks (s/keys :req [::syms]
                        :opt [:clojure.spec.test.check/num-tests
                              :clojure.spec.test.check/max-size]))

(s/def :kaocha.type/clojure.spec.test.alpha.check
  (s/keys :req [:kaocha.testable/type
                :kaocha.testable/id
                :kaocha/ns-patterns
                :kaocha/source-paths
                ::checks]
          :opt [:kaocha.filter/skip-meta]))

(hierarchy/derive! :kaocha.type/clojure.spec.test.alpha.check
                   :kaocha.testable.type/suite)

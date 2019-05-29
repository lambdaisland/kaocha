(ns kaocha.plugin.alpha.spec-check
  (:require [clojure.spec.alpha :as s]
            [kaocha.hierarchy :as kaocha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [kaocha.type.clojure.test]
            [kaocha.type :as type]))

(defplugin kaocha.plugin.alpha/spec-check
  (pre-load [{ns-patterns ::ns-patterns :as config}]
    (update config :kaocha/tests conj
            {:kaocha.testable/type    :kaocha.type/clojure.spec.test.alpha.check
             :kaocha.testable/id      :generative
             :kaocha/ns-patterns      ns-patterns
             :kaocha/source-paths     ["src"],
             :kaocha.filter/skip-meta [:kaocha/skip-spec-check
                                       :kaocha/skip]})))

(s/def ::ns-patterns :kaocha/ns-patterns)

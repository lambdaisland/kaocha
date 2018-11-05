(ns kaocha.test-factories
  (:require [kaocha.specs]
            [kaocha.api]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [kaocha.config :as config]))

(defn var-testable [m]
  (let [testable (gen/generate (s/gen :kaocha.type/var))]
    (merge testable
           {:kaocha.testable/type :kaocha.type/var
            :kaocha.testable/desc (name (:kaocha.testable/id testable))}
           m)))

(defn test-plan [m]
  (merge
   (config/default-config)

   m))

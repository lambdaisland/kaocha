(ns kaocha.test-factories
  (:require [kaocha.specs]
            [kaocha.api]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [kaocha.config :as config]))

(defn var-testable [m]
  (merge (gen/generate (s/gen :kaocha.type/var))
         {:kaocha.testable/type :kaocha.type/var}
         m))

(defn test-plan [m]
  (merge
   (config/default-config)
   m))

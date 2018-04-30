(ns kaocha.specs
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(def global-opts #{:kaocha/reporter
                   :kaocha/color?
                   :kaocha/randomize?
                   :kaocha/seed
                   :kaocha/suites
                   :kaocha/only-suites
                   :kaocha/fail-fast?
                   :kaocha/watch?})

(def suite-opts #{:kaocha/id
                  :kaocha/source-paths
                  :kaocha/test-paths
                  :kaocha/ns-patterns})

(s/def :kaocha/config (s/keys :req ~global-opts))

(s/def :kaocha/suites (s/coll-of :kaocha/suite))

(s/def :kaocha/suite (s/keys :req ~suite-opts))

(comment
  (require 'kaocha.config)


  (expound/expound :kaocha/config
                   (-> (kaocha.config/load-config)
                       kaocha.config/normalize)))

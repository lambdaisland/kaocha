(ns kaocha.plugin.alpha.spec-check
  (:require [clojure.spec.alpha :as s]
            [kaocha.hierarchy :as kaocha]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [kaocha.type.clojure.test]
            [kaocha.type :as type]
            [kaocha.type.clojure.spec.alpha.check]
            [kaocha.type.clojure.spec.alpha.fdef :as type.fdef]))

(alias 'stc 'clojure.spec.test.check)
(alias 'type.stc 'kaocha.type.clojure.spec.alpha.check)

(defn testable [{ns-patterns ::ns-patterns :as config}]
  (-> {:kaocha.testable/type    :kaocha.type/clojure.spec.test.alpha.check
       :kaocha.testable/id      :generative
       :kaocha/ns-patterns      ns-patterns
       :kaocha/source-paths     ["src"],
       :kaocha.filter/skip-meta [:kaocha/skip]
       ::type.stc/syms          :all-fdefs}
      (merge (type.fdef/stc-opts config))))

(defplugin kaocha.plugin.alpha/spec-check
  (pre-load [config] (update config :kaocha/tests conj (testable config)))
  (cli-options [opts]
    (conj opts
          [nil  "--num-tests NUM" "Test iterations per fdef"
           :parse-fn #(Integer/parseInt %)]
          [nil  "--max-size SIZE" "Maximum length of generated collections"
           :parse-fn #(Integer/parseInt %)]
          [nil  "--ns-pattern PATTERN" "Regex matching your project's namespaces"]))
  (config [config]
    (let [num-tests  (get-in config [:kaocha/cli-options :num-tests])
          max-size   (get-in config [:kaocha/cli-options :max-size])
          ns-pattern (get-in config [:kaocha/cli-options :ns-pattern])]
      (assoc config
             ::ns-patterns [ns-pattern]
             ::stc/num-tests num-tests
             ::stc/max-size max-size))))

(s/def ::ns-patterns :kaocha/ns-patterns)

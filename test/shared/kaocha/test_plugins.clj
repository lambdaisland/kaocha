(ns kaocha.test-plugins
  (:require [kaocha.plugin :as plugin]
            [kaocha.output :as output]
            [clojure.test :as t]
            [clojure.string :as str]
            [kaocha.hierarchy :as hierarchy]))

(hierarchy/derive! ::unexpected-error :kaocha/known-key)
(hierarchy/derive! ::unexpected-error :kaocha/fail-type)

(hierarchy/derive! ::unexpected-warning :kaocha/known-key)
(hierarchy/derive! ::unexpected-warning :kaocha/fail-type)

(plugin/defplugin ::capture-warn+error
  "Turn errors and warning messages into test failures."
  (wrap-run [run test-plan]
    (fn [& args]
      (let [warnings (atom [])
            errors (atom [])]
        (with-redefs [output/warn (fn [& xs] (swap! warnings conj xs))
                      output/error (fn [& xs] (swap! errors conj xs))]
          (let [result (apply run args)]
            (when (seq @errors)
              (#'t/do-report {:type ::unexpected-error
                              :message (str "Unexpected Error:\n"
                                            (str/join "\n" (map #(apply str "  ERROR: " %) @errors)))}))
            (when (seq @warnings)
              (#'t/do-report {:type ::unexpected-warning
                              :message (str "Unexpected Warning:\n"
                                            (str/join "\n" (map #(apply str "WARNING: " %) @warnings)))}))
            result))))))

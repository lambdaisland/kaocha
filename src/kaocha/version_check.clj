(ns kaocha.version-check
  (:require  [kaocha.output :as output]
            [slingshot.slingshot :refer [throw+]]))

(defn check-version-minimum 
  "Checks that Clojure has at least a minimum version"
  [major minor]
  (when-not (or (and (= (:major *clojure-version*) major) (>= (:minor *clojure-version*) minor))
                (>= (:major *clojure-version*) (inc major) ))
    (let [msg (format "Kaocha requires Clojure %d.%d or later." major minor)]
      (output/error msg)
      (throw+ {:kaocha/early-exit 251} msg))))

(check-version-minimum 1 9)

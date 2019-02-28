(ns kaocha.versions
  (:require [clojure.java.io :as io]
            [semver.core :as semver])
  (:import (java.util Properties)))

(defn get-version
  [dep]
  (let [path (str "META-INF/maven/" (or (namespace dep) (name dep))
                  "/" (name dep) "/pom.properties")
        props (io/resource path)]
    (when props
      (with-open [stream (io/input-stream props)]
        (let [props (doto (Properties.) (.load stream))]
          (.getProperty props "version"))))))

(def min-required-versions
  {'org.clojure/tools.cli "0.4.0"})

(defn check-versions!
  []
  (doseq [[dep version] min-required-versions]
    (when-not (semver/newer? (get-version dep) version)
      (throw (Exception. (format "Library %s is too old please upgrade to at least %s" (str dep) version))))))

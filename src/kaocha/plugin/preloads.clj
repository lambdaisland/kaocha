(ns kaocha.plugin.preloads
  (:require [kaocha.plugin :refer [defplugin]]))

(defplugin kaocha.plugin/preloads
  (pre-load [config]
    (when-let [ns-names (::ns-names config)]
      (apply require ns-names))
    config))

(ns kaocha.plugin.cloverage
  #_(:require [bultitude.core :as blt]
              [clojure.java.io :as io]
              [clojure.set :as set]
              [clojure.test :as test]
              [clojure.tools.cli :as cli]
              [clojure.test.junit :as junit]
              [clojure.tools.logging :as log]
              [cloverage.debug :as debug]
              [cloverage.dependency :as dep]
              [cloverage.instrument :as inst]
              [cloverage.report :as rep]
              [cloverage.report.console :as console]
              [cloverage.report.coveralls :as coveralls]
              [cloverage.report.codecov :as codecov]
              [cloverage.report.emma-xml :as emma-xml]
              [cloverage.report.html :as html]
              [cloverage.report.lcov :as lcov]
              [cloverage.report.raw :as raw]
              [cloverage.report.text :as text]
              [cloverage.source :as src])
  (:import clojure.lang.IObj))

#_
(defplugin kaocha.plugin/cloverage
  (cli-options [opts]
               (conj opts
                     [nil "--[no]-cloverage" "Enable cloverage." :default true]
                     [nil "--cloverage-output" "Cloverage output directory." :default "target/coverage"]))

  (config [config]
          (if-let [output (get-in config [:kaocha/cli-options :cloverage-output])]
            (assoc config ::output output)
            config))


  )

(ns kaocha.platform.classpath
  "On babashka we use bb's version of add-classpath"
  (:refer-clojure :exclude [add-classpath])
  (:require [babashka.classpath :as bbcp]))

(def add-classpath bbcp/add-classpath)

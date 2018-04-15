(ns lambdaisland.kaocha.test-util
  (:require [clojure.test :as t]))

(defmacro with-out-err
  "Captures the return value of the expression, as well as anything written on
  stdout or stderr."
  [& body]
  `(let [o# (java.io.StringWriter.)
         e# (java.io.StringWriter.)]
     (binding [*out* o#
               *err* e#]
       (let [r# (do ~@body)]
         {:out (str o#)
          :err (str e#)
          :result r#}))))

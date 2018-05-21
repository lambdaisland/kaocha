(ns kaocha.test-util
  (:require [clojure.test :as t]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [matcher-combinators.test :as mc.test]
            [matcher-combinators.matchers :as mc.match]
            [matcher-combinators.core :as mc.core]))

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


;; This is a bit of an experiment: provide some better assert-expr out of the box

#_
(defmethod t/assert-expr '= [msg form]
  `(let [[expected# actual#] (list ~@(rest form))
         result#             (mc.core/match (mc.match/equals expected#) actual#)]
     (clojure.test/do-report
      (if (mc.core/match? result#)
        {:type     :pass
         :message  ~msg
         :expected '~form
         :actual   (list '= expected# expected#)}
        (mc.test/with-file+line-info
          {:type     :mismatch
           :message  ~msg
           :expected '~form
           :actual   (list '~'not (list '= expected# actual#))
           :markup   (second result#)})))))

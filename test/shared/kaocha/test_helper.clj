(ns kaocha.test-helper
  (:refer-clojure :exclude [symbol])
  (:require [clojure.test :as t]
            [kaocha.core-ext :refer :all]
            [matcher-combinators.result :as mc.result]
            [matcher-combinators.core :as mc.core]
            [matcher-combinators.model :as mc.model]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [orchestra.spec.test :as orchestra])
  (:import [clojure.lang ExceptionInfo]))

(orchestra/instrument)

(extend-protocol mc.core/Matcher
  clojure.lang.Var
  (match [this actual]
    (if (= this actual)
      {::mc.result/type :match
       ::mc.result/value actual
       ::mc.result/weight 0}
      {::mc.result/type :mismatch
       ::mc.result/value (if (and (keyword? actual) (= ::mc.core/missing actual))
                           (mc.model/->Missing this)
                           (mc.model/->Mismatch this actual))
       ::mc.result/weight 1})))

;; TODO move to kaocha.assertions

(defmacro thrown-ex-data?
  "Verifies that an expression throws an ExceptionInfo with specific data and
  message. Message can be string or regex. "
  [ex-msg ex-data & body]
  (assert nil "thrown-ex-data? used outside (is ) block"))

(defmethod clojure.test/assert-expr 'thrown-ex-data? [msg form]
  (let [[_ ex-msg ex-data & body] form]
    `(try
       ~@body
       (t/do-report {:type :fail
                     :message ~msg
                     :expected '~form
                     :actual nil})
       (catch ExceptionInfo e#
         (let [m# (.getMessage e#)
               d# (clojure.core/ex-data e#)]
           (cond
             (not (or (and (string? ~ex-msg) (= ~ex-msg m#))
                      (and (regex? ~ex-msg) (re-find ~ex-msg m#))))
             (t/do-report {:type :fail
                           :message ~msg
                           :expected '~ex-msg
                           :actual m#})

             (not= ~ex-data d#)
             (t/do-report {:type :fail
                           :message ~msg
                           :expected ~ex-data
                           :actual d#})

             :else
             (t/do-report {:type :pass
                           :message ~msg
                           :expected '~form
                           :actual e#})))
         true))))

(defn spec-valid?
  "Asserts that the value matches the spec."
  [spec value & [msg]]
  (s/valid? spec value))

(defmethod t/assert-expr 'spec-valid? [msg form]
  `(let [[spec# value#] (list ~@(rest form))]
     (t/do-report
      (if (s/valid? spec# value#)
        {:type :pass
         :message ~msg
         :expected '~form
         :actual '~form}
        {:type :fail
         :message (or ~msg (expound/expound-str spec# value#))
         :expected '~form
         :actual (list '~'not '~form)}))))

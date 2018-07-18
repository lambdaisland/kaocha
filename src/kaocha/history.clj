(ns kaocha.history
  (:require [clojure.test :as t]))

;; TODO: this tries to track progress separate of t/inc-report-counter, which
;; doesn't always work, see e.g. :mismatch to support matcher-combinators

(def ^:dynamic *history* nil)

(defmulti track :type)

(defmethod track :default [m] (swap! *history* conj m))

(defmethod track :fail [m]
  (swap! *history* conj (assoc m
                               :testing-contexts t/*testing-contexts*
                               :testing-vars t/*testing-vars*)) )

(defmethod track :error [m]
  (swap! *history* conj (assoc m
                               :testing-contexts t/*testing-contexts*
                               :testing-vars t/*testing-vars*)))

(defn clojure-test-summary
  ([]
   (clojure-test-summary @*history*))
  ([history]
   (reduce
    (fn [m {type :type}]
      (cond
        (= type :begin-test-var)            (update m :test inc)
        (= type :mismatch)                  (update m :fail inc)
        (some #{type} [:pass :fail :error]) (update m type inc)
        :else                               m))
    {:type  :summary
     :test  0
     :pass  0
     :fail  0
     :error 0}
    history)))

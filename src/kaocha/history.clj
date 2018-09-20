(ns kaocha.history
  (:require [clojure.test :as t]))

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
        (= type :begin-test-var)               (update m :test inc)
        (= type :matcher-combinators/mismatch) (update m :fail inc)
        (= type :mismatch)                     (update m :fail inc)
        (some #{type} [:pass :fail :error])    (update m type inc)
        :else                                  m))
    {:type  :summary
     :test  0
     :pass  0
     :fail  0
     :error 0}
    history)))

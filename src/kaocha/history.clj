(ns kaocha.history
  (:require [clojure.test :as t]
            [kaocha.hierarchy :as hierarchy]))

(def ^:dynamic *history* nil)

(defmulti track :type :hierarchy #'hierarchy/hierarchy)

(defmethod track :default [m]
  (when *history* (swap! *history* conj m)))

(defmethod track :kaocha/fail-type [m]
  (when *history*
    (swap! *history* conj (assoc m
                                 :testing-contexts t/*testing-contexts*
                                 :testing-vars t/*testing-vars*))) )

(defmethod track :error [m]
  (when *history*
    (swap! *history* conj (assoc m
                                 :testing-contexts t/*testing-contexts*
                                 :testing-vars t/*testing-vars*))))

(defn clojure-test-summary
  ([]
   (when *history*
     (clojure-test-summary @*history*)))
  ([history]
   (reduce
    (fn [m {type :type :as event}]
      (cond
        (some #{type} [:pass :error :kaocha/pending]) (update m type inc)
        (hierarchy/isa? type :kaocha/begin-test)      (update m :test inc)
        (hierarchy/fail-type? event)                  (update m :fail inc)
        :else                                         m))
    {:type  :summary
     :test  0
     :pass  0
     :fail  0
     :error 0}
    history)))

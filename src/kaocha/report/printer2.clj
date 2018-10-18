(ns kaocha.report.printer2
  (:require [puget.printer :as puget]
            [puget.dispatch]
            [arrangement.core]
            [fipp.visit :as fv]
            [fipp.engine :as fipp]
            [puget.color :as color]
            [kaocha.report.diff :as diff]
            [kaocha.output :as out]))

(defn print-deletion [printer expr]
  (let [no-color (assoc printer :print-color false)]
    (color/document printer ::deletion [:span "-" (puget/format-doc no-color (:- expr))])))

(defn print-insertion [printer expr]
  (let [no-color (assoc printer :print-color false)]
    (color/document printer ::insertion [:span "+" (puget/format-doc no-color (:+ expr))])))

(defn map-handler [this value]
  (let [ks (#'puget/order-collection (:sort-keys this) value (partial sort-by first arrangement.core/rank))
        entries (map (partial puget/format-doc this) ks)]
    [:group
     (color/document this :delimiter "{")
     [:align (interpose [:span (:map-delimiter this) :line] entries)]
     (color/document this :delimiter "}")]))

(def print-handlers {kaocha.report.diff.Deletion
                     print-deletion

                     kaocha.report.diff.Insertion
                     print-insertion

                     kaocha.report.diff.Mismatch
                     (fn [printer expr]
                       [:group
                        [:align
                         (print-deletion printer expr) :line
                         (print-insertion printer expr)]])

                     clojure.lang.PersistentArrayMap
                     map-handler

                     clojure.lang.PersistentHashMap
                     map-handler

                     clojure.lang.MapEntry
                     (fn [printer value]
                       (let [k (key value)
                             v (val value)]
                         (let [no-color (assoc printer :print-color false)]
                           (cond
                             (instance? kaocha.report.diff.Insertion k)
                             [:span
                              (print-insertion printer k)
                              (if (coll? v) (:map-coll-separator printer) " ")
                              (color/document printer ::insertion (puget/format-doc no-color v))]

                             (instance? kaocha.report.diff.Deletion k)
                             [:span
                              (print-deletion printer k)
                              (if (coll? v) (:map-coll-separator printer) " ")
                              (color/document printer ::deletion (puget/format-doc no-color v))]

                             :else
                             [:span
                              (puget/format-doc printer k)
                              (if (coll? v) (:map-coll-separator printer) " ")
                              (puget/format-doc printer v)]))))})

(defn puget-printer []
  (puget/pretty-printer {:width 20
                         :print-color out/*colored-output*
                         :color-scheme {::deletion [:red]
                                        ::insertion [:green]}
                         :print-handlers print-handlers}))

(defn format-doc [expr]
  (puget/format-doc (puget-printer) expr))

(defn print-doc [doc]
  (fipp.engine/pprint-document doc {:width 20}))

(ns kaocha.report.printer
  (:require [puget.printer :as puget]
            [puget.dispatch]
            [arrangement.core]
            [fipp.visit :as fv]
            [fipp.engine :as fipp]
            [puget.color :as color]
            [kaocha.report.diff :as diff]
            [kaocha.output :as output]))

(defn print-deletion [printer expr]
  (let [no-color (assoc printer :print-color false)]
    (color/document printer ::deletion [:span "-" (puget/format-doc no-color (:- expr))])))

(defn print-insertion [printer expr]
  (let [no-color (assoc printer :print-color false)]
    (color/document printer ::insertion [:span "+" (puget/format-doc no-color (:+ expr))])))

(defn print-mismatch [printer expr]
  [:group
   [:span ""] ;; needed here to make this :nest properly in kaocha.report/print-expr '=
   [:align
    (print-deletion printer expr) :line
    (print-insertion printer expr)]])

(defn print-other [printer expr]
  (let [no-color (assoc printer :print-color false)]
    (color/document printer ::other [:span "-" (puget/format-doc no-color expr)])))

(defn map-handler [this value]
  (let [ks (#'puget/order-collection (:sort-keys this) value (partial sort-by first arrangement.core/rank))
        entries (map (partial puget/format-doc this) ks)]
    [:group
     (color/document this :delimiter "{")
     [:align (interpose [:span (:map-delimiter this) :line] entries)]
     (color/document this :delimiter "}")]))

(def print-handlers {'kaocha.report.diff.Deletion
                     print-deletion

                     'kaocha.report.diff.Insertion
                     print-insertion

                     'kaocha.report.diff.Mismatch
                     print-mismatch

                     'clojure.lang.PersistentArrayMap
                     map-handler

                     'clojure.lang.PersistentHashMap
                     map-handler

                     'clojure.lang.MapEntry
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
  (puget/pretty-printer {:width          (or *print-length* 100)
                         :print-color    output/*colored-output*
                         :color-scheme   {::deletion  [:red]
                                          ::insertion [:green]
                                          ::other     [:yellow]
                                          ;; puget uses green and red for
                                          ;; boolean/tag, but we want to reserve
                                          ;; those for diffed values.
                                          :boolean    [:bold :cyan]
                                          :tag        [:magenta]}
                         :print-handlers (fn [klz]
                                           (get @#'print-handlers (symbol (.getName klz))))}))

(defn format-doc [expr]
  (puget/format-doc (puget-printer) expr))

(defn print-doc [doc]
  (fipp.engine/pprint-document doc {:width (or *print-length* 100)}))

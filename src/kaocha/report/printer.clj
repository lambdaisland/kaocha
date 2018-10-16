(ns kaocha.report.printer
  (:require [kaocha.report.diff :as diff]
            [clojure.pprint :as pprint]
            [kaocha.output :as out]))

(def ^:dynamic *in-color* false)

(defrecord ColorTag [color expr])

(defmulti print-diff-dispatch type)

(defn override-color [color expr]
  (binding [*in-color* true]
    (pprint/write-out (->ColorTag color expr))))

(defmethod print-diff-dispatch ColorTag [{:keys [color expr]}]
  (print (out/colors color))
  (pprint/write-out expr)
  (print (out/colors :reset)))

(defmethod print-diff-dispatch kaocha.report.diff.Deletion [expr]
  (pprint/write-out (->ColorTag :red '-))
  (override-color :red (:- expr)))

(defmethod print-diff-dispatch kaocha.report.diff.Insertion [expr]
  (pprint/write-out (->ColorTag :green '+))
  (override-color :green (:+ expr)))

(defmethod print-diff-dispatch kaocha.report.diff.Mismatch [expr]
  (pprint/write-out (diff/->Deletion (:- expr)))
  (.write ^java.io.Writer *out* " ")
  (pprint/pprint-newline :linear)
  (pprint/write-out (diff/->Insertion (:+ expr))))

(defn pprint-map-entry [k v]
  (pprint/write-out k)
  (.write ^java.io.Writer *out* " ")
  (pprint/pprint-newline :linear)
  (binding [pprint/*current-length* 0] ; always print both parts of the [k v] pair
    (pprint/write-out
     (cond
       (instance? kaocha.report.diff.Deletion k)
       (->ColorTag :red v)
       (instance? kaocha.report.diff.Insertion k)
       (->ColorTag :green v)
       :else
       v))))

(defmethod print-diff-dispatch clojure.lang.IPersistentMap [amap]
  (let [[ns lift-map] (when (not (record? amap))
                        (#'clojure.core/lift-ns amap))
        amap (or lift-map amap)
        prefix (if ns (str "#:" ns "{") "{")]
    (pprint/pprint-logical-block
     :prefix prefix :suffix "}"
     (pprint/print-length-loop [aseq (seq amap)]
                               (when aseq
                                 (pprint/pprint-logical-block
                                  (let [k (ffirst aseq)
                                        v (fnext (first aseq))]
                                    (pprint-map-entry k v)))
                                 (when (next aseq)
                                   (.write ^java.io.Writer *out* ", ")
                                   (pprint/pprint-newline :linear)
                                   (recur (next aseq))))))))

(defmethod print-diff-dispatch clojure.lang.Keyword [expr]
  (when-not *in-color* (.write ^java.io.Writer *out* (out/colors :blue)))
  (pprint/simple-dispatch expr)
  (when-not *in-color* (.write ^java.io.Writer *out* (out/colors :reset))))

(defmethod print-diff-dispatch :default [expr]
  (pprint/simple-dispatch expr))

(defn pretty-print [expr]
  (pprint/with-pprint-dispatch
    print-diff-dispatch
    (pprint/pprint expr)))

(comment
  (pretty-print
   (diff/diff {:x {:aaaa 1 :bbbbbb [[ 1 2 3 4 5 6]] :cccccc 3333 :ddddddd 44444 :kkkkkk 6666}}
              {:x {:aaaa 1 :bbbbbb ["fooobarararrararara"] :xx 6}})))

(ns kaocha.report.diff
  (:require [clojure.data :as data]
            [clj-diff.core :as seq-diff]))

(declare diff)

(defrecord Mismatch [- +])
(defrecord Deletion [-])
(defrecord Insertion [+])

(defprotocol Diff
  (diff-similar [x y]))

;; For property based testing
(defprotocol Undiff
  (left-undiff [x])
  (right-undiff [x]))

(defn replacements [del ins]
  (reduce (fn [m d]
            (if-let [i (some ins #{d (dec d)})]
              (assoc m d (first i))
              m)) {} del))

(defn diff-seq [exp act]
  (first
   (let [{del :- ins :+} (seq-diff/diff exp act)
         del (into #{} del)
         ins (into {} (map (fn [[k & vs]] [k (vec vs)])) ins)
         rep (replacements del ins)
         del (apply disj del (keys rep))
         ins (reduce (fn [m [k vs]]
                       (if (or (contains? rep k) (contains? rep (inc k)))
                         (if (next vs)
                           (assoc m k (next vs))
                           m)
                         (assoc m k vs)))
                     {}
                     ins)]
     (prn {:ins ins :del del :rep rep})
     (reduce
      (fn [[s idx] x]
        [(cond-> s
           (contains? rep idx)
           (conj (diff (nth exp idx) (get rep idx)))

           (contains? del idx)
           (conj (->Deletion x))

           (not (or  (contains? del idx) (contains? rep idx)))
           (conj x)

           (contains? ins idx)
           (into (map ->Insertion) (get ins idx)))
         (inc idx)])
      [(if (contains? ins -1)
         (into [] (map ->Insertion (get ins -1)))
         []) 0]
      exp))))

(defn diff-map [exp act]
  (first
   (let [exp-ks (sort (keys exp))
         act-ks (sort (keys act))
         {del :- ins :+} (seq-diff/diff exp-ks act-ks)
         del (into #{} del)
         ins (into {} (map (fn [[k & vs]] [k (vec vs)])) ins)]

     (reduce
      (fn [[m idx] k]
        [(cond-> m
           (contains? del idx)
           (assoc (->Deletion k) (exp k))

           (not (contains? del idx))
           (assoc k (diff (exp k) (act k)))

           (contains? ins idx)
           (into (map (juxt ->Insertion act)) (get ins idx)))
         (inc idx)])
      [(if (contains? ins -1)
         (into {} (map (juxt ->Insertion act)) (get ins -1))
         {}) 0]
      exp-ks))))

(defn diff-atom [exp act]
  (if (= exp act)
    exp
    (->Mismatch exp act)))

(defn diff [exp act]
  (if (= (data/equality-partition exp) (data/equality-partition act))
    (diff-similar exp act)
    (diff-atom exp act)))

(extend nil
  Diff
  {:diff-similar diff-atom})

(extend Object
  Diff
  {:diff-similar (fn [exp act]
                   (if (.isArray (.getClass exp))
                     (diff-seq exp act)
                     (diff-atom exp act)))})

(extend-protocol Diff
  java.util.List
  (diff-similar [exp act] (diff-seq exp act))

  java.util.Set
  (diff-similar [exp act] (set (diff-seq (sort exp) (sort act))))

  java.util.Map
  (diff-similar [exp act] (diff-map exp act)))

(extend-protocol Undiff
  java.util.List
  (left-undiff [s] (map left-undiff (remove #(instance? Insertion %) s)))
  (right-undiff [s] (map right-undiff (remove #(instance? Deletion %) s)))

  java.util.Set
  (left-undiff [s] (set (left-undiff (seq s))))
  (right-undiff [s] (set (right-undiff (seq s))))

  java.util.Map
  (left-undiff [m]
    (into {}
          (comp (remove #(instance? Insertion (key %)))
                (map (juxt (comp left-undiff key) (comp left-undiff val))))
          m))
  (right-undiff [m]
    (into {}
          (comp (remove #(instance? Deletion (key %)))
                (map (juxt (comp right-undiff key) (comp right-undiff val))))
          m))

  Mismatch
  (left-undiff [m] (get m :-))
  (right-undiff [m] (get m :+))

  Insertion
  (right-undiff [m] (get m :+))

  Deletion
  (left-undiff [m] (get m :-)))

(extend nil Undiff {:left-undiff identity :right-undiff identity})
(extend Object Undiff {:left-undiff identity :right-undiff identity})

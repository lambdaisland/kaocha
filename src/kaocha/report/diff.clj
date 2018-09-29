(ns kaocha.report.diff
  (:require [clojure.data :as data]
            [clj-diff.core :as seq-diff]))

(declare diff)

(defrecord Mismatch [- +])
(defrecord Deletion [-])
(defrecord Insertion [+])

(defprotocol Diff
  (diff-similar [x y]))


(seq-diff/diff [1 2 3] [1 :x 3])
;; => {:+ [[0 :x]], :- [1]}

(seq-diff/diff [1 2 3] [1 :x 3])
(seq-diff/diff [1 2 3] [1 2 :x])
(seq-diff/diff [1 2 3] [1 2 :x :y])
(seq-diff/diff [1 2 3] [1 :c 2 :x :y])
(seq-diff/diff [1 2 3 4 5] [1 :x :y 3 :c 5])

(let [{del :- ins :+} (seq-diff/diff [1 2 3 4 5] [1 :x :y 3 :c 5] #_#_[1 2 3] [1 :x 3])
      del (into #{} del)
      ins (into {} (map (fn [[k & vs]] [k (vec vs)])) ins)]
  (replacements del ins))

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

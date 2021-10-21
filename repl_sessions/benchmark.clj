
(ns benchmark
  (:require [criterium.core :as c])
  (:import [java.util.concurrent Executors ])
  )

(def thread-pool (Executors/newFixedThreadPool 10))

(defn math-direct []
  (+ 1 1))

(defn math-future []
  (deref 
    (future (+ 1 1))))

(defn math-thread []
  (let [result (atom nil)]
    (doto (Thread. (fn [] (reset! result (+ 1 1))))
      (.start)
      (.join))
    @result))

(defn math-threadpool []
  (let [result (atom nil)]
    (.get (.submit thread-pool (fn [] (reset! result (+ 1 1))) ))
    @result))

(defn math-threadpool-no-atom []
  (.get (.submit thread-pool (fn [] (+ 1 1)) )))


(c/bench (math-direct) )
; (out) Evaluation count : 6215391600 in 60 samples of 103589860 calls.
; (out)              Execution time mean : 2,015262 ns
; (out)     Execution time std-deviation : 0,497743 ns
; (out)    Execution time lower quantile : 1,442374 ns ( 2,5%)
; (out)    Execution time upper quantile : 3,392990 ns (97,5%)
; (out)                    Overhead used : 7,915626 ns
; (out) 
; (out) Found 5 outliers in 60 samples (8,3333 %)
; (out) 	low-severe	 3 (5,0000 %)
; (out) 	low-mild	 2 (3,3333 %)
; (out)  Variance from outliers : 94,6147 % Variance is severely inflated by outliers

(c/bench (math-future) )
; (out) Evaluation count : 3735420 in 60 samples of 62257 calls.
; (out)              Execution time mean : 16,635809 µs
; (out)     Execution time std-deviation : 1,104338 µs
; (out)    Execution time lower quantile : 15,397518 µs ( 2,5%)
; (out)    Execution time upper quantile : 19,751883 µs (97,5%)
; (out)                    Overhead used : 7,915626 ns
; (out) 
; (out) Found 6 outliers in 60 samples (10,0000 %)
; (out) 	low-severe	 3 (5,0000 %)
; (out) 	low-mild	 3 (5,0000 %)
; (out)  Variance from outliers : 50,0892 % Variance is severely inflated by outliers

(c/bench (math-thread))

; (out) Evaluation count : 774420 in 60 samples of 12907 calls.
; (out)              Execution time mean : 82,513236 µs
; (out)     Execution time std-deviation : 5,706987 µs
; (out)    Execution time lower quantile : 75,772237 µs ( 2,5%)
; (out)    Execution time upper quantile : 91,971212 µs (97,5%)
; (out)                    Overhead used : 7,915626 ns
; (out) 
; (out) Found 1 outliers in 60 samples (1,6667 %)
; (out) 	low-severe	 1 (1,6667 %)
; (out)  Variance from outliers : 51,7849 % Variance is severely inflated by outliers

(c/bench (math-threadpool))
; (out) Evaluation count : 3815100 in 60 samples of 63585 calls.
; (out)              Execution time mean : 16,910124 µs
; (out)     Execution time std-deviation : 2,443261 µs
; (out)    Execution time lower quantile : 14,670118 µs ( 2,5%)
; (out)    Execution time upper quantile : 23,743868 µs (97,5%)
; (out)                    Overhead used : 7,915626 ns
; (out) 
; (out) Found 3 outliers in 60 samples (5,0000 %)
; (out) 	low-severe	 2 (3,3333 %)
; (out) 	low-mild	 1 (1,6667 %)
; (out)  Variance from outliers : 82,4670 % Variance is severely inflated by outliers


(c/bench (math-threadpool-no-atom))

; (out) Evaluation count : 3794940 in 60 samples of 63249 calls.
; (out)              Execution time mean : 16,182655 µs
; (out)     Execution time std-deviation : 1,215451 µs
; (out)    Execution time lower quantile : 14,729393 µs ( 2,5%)
; (out)    Execution time upper quantile : 18,549902 µs (97,5%)
; (out)                    Overhead used : 7,915626 ns
; (out) 
; (out) Found 3 outliers in 60 samples (5,0000 %)
; (out) 	low-severe	 2 (3,3333 %)
; (out) 	low-mild	 1 (1,6667 %)
; (out)  Variance from outliers : 56,7625 % Variance is severely inflated by outliers

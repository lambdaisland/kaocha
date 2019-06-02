(ns kaocha.stacktrace
  (:require [clojure.stacktrace :as st]
            [clojure.string :as str]))

(def ^:dynamic *stacktrace-filters* ["java.lang."
                                     "clojure.test$"
                                     "clojure.lang."
                                     "clojure.core"
                                     "clojure.main"
                                     "orchestra."
                                     "kaocha.monkey_patch"])

(defn elide-element? [e]
  (some #(str/starts-with? (.getClassName ^StackTraceElement e) %) *stacktrace-filters*))

(defn print-stack-trace
  "Prints a Clojure-oriented stack trace of tr, a Throwable.
  Prints a maximum of n stack frames (default: unlimited). Does not print
  chained exceptions (causes)."
  ([tr]
   (print-stack-trace tr nil))
  ([^Throwable tr n]
   (let [st (.getStackTrace tr)]
     (st/print-throwable tr)
     (newline)
     (print " at ")
     (if-let [e (first st)]
       (st/print-trace-element e) ;; always print the first element
       (print "[empty stack trace]"))
     (newline)
     (loop [[e & st] (next st)
            eliding? false
            n        n]
       (when e
         (let [n (cond-> n n dec)]
           (if (= 0 n)
             (println "    ... and " (count st) "more")
             (if (elide-element? e)
               (do
                 (when (not eliding?)
                   (println "    ..."))
                 (recur st true n))
               (do
                 (print "    ")
                 (st/print-trace-element e)
                 (newline)
                 (recur st false n))))))))))

(defn print-cause-trace
  "Like print-stack-trace but prints chained exceptions (causes)."
  ([tr]
   (print-cause-trace tr nil))
  ([tr n]
   (print-stack-trace tr n)
   (when-let [cause (.getCause ^Throwable tr)]
     (print "Caused by: ")
     (recur cause n))))

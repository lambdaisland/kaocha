(ns kaocha.classpath
  "This is the add-classpath function from Pomegranate 1.0.0, extracted so we
  don't need to pull in Aether."
  (:refer-clojure :exclude [add-classpath])
  (:require #_[dynapath.util :as dp]
            [kaocha.jit :as jit]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pomegranate

(defn ensure-compiler-loader
  "Ensures the clojure.lang.Compiler/LOADER var is bound to a DynamicClassLoader,
  so that we can add to Clojure's classpath dynamically."
  []
  #_(when-not (bound? Compiler/LOADER)
    (.bindRoot Compiler/LOADER (clojure.lang.DynamicClassLoader. (clojure.lang.RT/baseLoader)))))

(defn- classloader-hierarchy
  "Returns a seq of classloaders, with the tip of the hierarchy first.
   Uses the current thread context ClassLoader as the tip ClassLoader
   if one is not provided."
  ([]
   (ensure-compiler-loader)
   #_(classloader-hierarchy (deref clojure.lang.Compiler/LOADER)))
  ([tip]
   (->> tip
        (iterate #(.getParent ^ClassLoader %))
        (take-while boolean))))

(defn- modifiable-classloader?
  "Returns true iff the given ClassLoader is of a type that satisfies
   the dynapath.dynamic-classpath/DynamicClasspath protocol, and it can
   be modified."
  [cl]
  ((jit/jit dynapath.util/addable-classpath?) cl))

(defn add-classpath
  "A corollary to the (deprecated) `add-classpath` in clojure.core. This implementation
   requires a java.io.File or String path to a jar file or directory, and will attempt
   to add that path to the right classloader (with the search rooted at the current
   thread's context classloader)."
  ([jar-or-dir classloader]
   (if-not ((jit/jit dynapath.utill/add-classpath-url) classloader (.toURL (.toURI (io/file jar-or-dir))))
     (throw (IllegalStateException. (str classloader " is not a modifiable classloader")))))
  ([jar-or-dir]
   (let [classloaders (classloader-hierarchy)]
     (if-let [cl (filter modifiable-classloader? classloaders)]
       ;; Add to all classloaders that allow it. Brute force but doesn't hurt.
       (run! #(add-classpath jar-or-dir %) cl)
       (throw (IllegalStateException. (str "Could not find a suitable classloader to modify from "
                                           classloaders)))))))

;; /Pomegranate
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

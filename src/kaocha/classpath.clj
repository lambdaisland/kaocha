(ns kaocha.classpath
  "This is the add-classpath function from Pomegranate 1.0.0, extracted so we
  don't need to pull in Aether."
  (:refer-clojure :exclude [add-classpath])
  (:require [dynapath.util :as dp]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pomegranate

(defn- classloader-hierarchy
  "Returns a seq of classloaders, with the tip of the hierarchy first.
   Uses the current thread context ClassLoader as the tip ClassLoader
   if one is not provided."
  ([] (classloader-hierarchy (.. Thread currentThread getContextClassLoader)))
  ([tip]
   (->> tip
        (iterate #(.getParent %))
        (take-while boolean))))

(defn- modifiable-classloader?
  "Returns true iff the given ClassLoader is of a type that satisfies
   the dynapath.dynamic-classpath/DynamicClasspath protocol, and it can
   be modified."
  [cl]
  (dp/addable-classpath? cl))

(defn add-classpath
  "A corollary to the (deprecated) `add-classpath` in clojure.core. This implementation
   requires a java.io.File or String path to a jar file or directory, and will attempt
   to add that path to the right classloader (with the search rooted at the current
   thread's context classloader)."
  ([jar-or-dir classloader]
   (if-not (dp/add-classpath-url classloader (.toURL (.toURI (io/file jar-or-dir))))
     (throw (IllegalStateException. (str classloader " is not a modifiable classloader")))))
  ([jar-or-dir]
   (let [classloaders (classloader-hierarchy)]
     (if-let [cl (last (filter modifiable-classloader? classloaders))]
       (add-classpath jar-or-dir cl)
       (throw (IllegalStateException. (str "Could not find a suitable classloader to modify from "
                                           classloaders)))))))

;; /Pomegranate
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn maybe-add-dynamic-classloader
  "Add a DynamicClassLoader to the hierarchy if none of the existing classloaders
  are modifiable."
  []
  (when-not (some modifiable-classloader? (classloader-hierarchy))
    (let [thread        (Thread/currentThread)
          contextloader (.getContextClassLoader thread)
          classloader   (clojure.lang.DynamicClassLoader. contextloader)]
      (.setContextClassLoader thread classloader))))

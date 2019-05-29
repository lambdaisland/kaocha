(ns kaocha.core-ext
  "Core language extensions"
  (:refer-clojure :exclude [symbol])
  (:require [clojure.string :as str])
  (:import [java.util.regex Pattern]))

(defn regex? [x]
  (instance? Pattern x))

(defn exception? [x]
  (instance? java.lang.Exception x))

(defn error? [x]
  (instance? java.lang.Error x))

(defn throwable? [x]
  (instance? java.lang.Throwable x))

(defn ns? [x]
  (instance? clojure.lang.Namespace x))

(defn file? [x]
  (and (instance? java.io.File x) (.isFile x)))

(defn directory? [x]
  (and (instance? java.io.File x) (.isDirectory x)))

(defn path? [x]
  (instance? java.nio.file.Path x))

(defn regex
  ([x & xs]
   (regex (apply str x xs)))
  ([x]
   (cond
     (regex? x)  x
     (string? x) (Pattern/compile x)
     :else       (throw (ex-info (str "Can't coerce " (class x) " to regex.") {})))))

(defn mapply
  "Applies a function f to the argument list formed by concatenating
  everything but the last element of args with the last element of
  args.  This is useful for applying a function that accepts keyword
  arguments to a map."
  [f & args]
  (apply f (apply concat (butlast args) (last args))))

(defn symbol
  "Backport from Clojure 1.10, symbol function that's a bit more lenient on its
  inputs.

  Returns a Symbol with the given namespace and name. Arity-1 works on strings,
  keywords, and vars."
  ^clojure.lang.Symbol
  ([name]
   (cond
     (symbol? name) name
     (instance? String name) (clojure.lang.Symbol/intern name)
     (instance? clojure.lang.Var name) (.toSymbol ^clojure.lang.Var name)
     (instance? clojure.lang.Keyword name) (.sym ^clojure.lang.Keyword name)
     :else (throw (IllegalArgumentException. "no conversion to symbol"))))
  ([ns name] (clojure.lang.Symbol/intern ns name)))

(defn in-namespace? [ns-name sym-or-kw]
  (-> sym-or-kw namespace (str/starts-with? ns-name)))

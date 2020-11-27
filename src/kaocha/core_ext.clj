(ns kaocha.core-ext
  "Core language extensions"
  (:refer-clojure :exclude [symbol])
  (:import [java.util.regex Pattern])
  (:require  [kaocha.output :as output]
            [slingshot.slingshot :refer [try+ throw+]]))

(when-not (and (>= (:major *clojure-version*) 1) (>= (:minor *clojure-version*) 9))
                       (output/error "Kaocha requires Clojure 1.9 or later.")
                       (throw+ {:kaocha/early-exit 251}))

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
  (and (instance? java.io.File x) (.isFile ^java.io.File x)))

(defn directory? [x]
  (and (instance? java.io.File x) (.isDirectory ^java.io.File x)))

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

;; 1.10 backport
(when-not (resolve 'clojure.core/requiring-resolve)
  ;; using defn generates a warning even when not evaluated
  (intern *ns*
          ^{:doc "Resolves namespace-qualified sym per 'resolve'. If initial resolve
  fails, attempts to require sym's namespace and retries."}
          'requiring-resolve
          (fn [sym]
            (if (qualified-symbol? sym)
              (or (resolve sym)
                  (do (-> sym namespace symbol require)
                      (resolve sym)))
              (throw (IllegalArgumentException. (str "Not a qualified symbol: " sym)))))))

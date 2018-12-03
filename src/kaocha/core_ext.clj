(ns kaocha.core-ext
  "Core language extensions"
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

(defn regex [x]
  (cond
    (regex? x)  x
    (string? x) (Pattern/compile x)
    :else       (throw (ex-info (str "Can't coerce " (class x) " to regex.") {}))))

(defn mapply
  "Applies a function f to the argument list formed by concatenating
  everything but the last element of args with the last element of
  args.  This is useful for applying a function that accepts keyword
  arguments to a map."
  [f & args]
  (apply f (apply concat (butlast args) (last args))))

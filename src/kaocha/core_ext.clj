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

(defn regex [x]
  (cond
    (regex? x)  x
    (string? x) (Pattern/compile x)
    :else       (throw (ex-data (str "Can't coerce " (class x) " to regex." {})))))

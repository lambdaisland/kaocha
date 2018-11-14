(ns kaocha.output
  (:require [kaocha.jit :refer [jit]]))

(def ^:dynamic *colored-output* true)

(def ESC \u001b)

(def colors
  {:black     (str ESC "[30m")
   :red-bg    (str ESC "[41m")
   :red       (str ESC "[31m")
   :green     (str ESC "[32m")
   :yellow    (str ESC "[33m")
   :blue      (str ESC "[34m")
   :magenta   (str ESC "[35m")
   :cyan      (str ESC "[36m")
   :white     (str ESC "[37m")
   :underline (str ESC "[4m")
   :reset     (str ESC "[m")})

(defn colored [color string]
  (if *colored-output*
    (str (get colors color) string (:reset colors))
    string))

(defn warn [& args]
  (binding [*out* *err*]
    (println (apply str (colored :yellow "WARNING: ") args))))

(defn error [& args]
  (binding [*out* *err*]
    (println (apply str (colored :red "ERROR: ") args))))

(defn printer [& [opts]]
  ((jit lambdaisland.deep-diff/printer) (merge {:print-color *colored-output*} opts)))

(defn print-doc
  ([doc]
   (print-doc doc (printer)))
  ([doc printer]
   ((jit fipp.engine/pprint-document) doc {:width (:width printer)})))

(defn format-doc
  ([doc]
   (format-doc doc (printer)))
  ([doc printer]
   ((jit puget.printer/format-doc) printer doc)))

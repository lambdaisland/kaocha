(ns kaocha.output
  (:require [io.aviso.ansi :as ansi]
            [lambdaisland.deep-diff :as ddiff]
            [fipp.engine :as fipp]
            [puget.printer :as puget]))

(def ^:dynamic *colored-output* true)

(def colors
  {:black   ansi/black-font
   :red-bg  ansi/red-bg-font
   :red     ansi/red-font
   :green   ansi/green-font
   :yellow  ansi/yellow-font
   :blue    ansi/blue-font
   :magenta ansi/magenta-font
   :cyan    ansi/cyan-font
   :white   ansi/white-font
   :reset   ansi/reset-font})

(defn colored [color string]
  (if *colored-output*
    (str (get colors color) string ansi/reset-font)
    string))

(defn warn [& args]
  (binding [*out* *err*]
    (println (apply str (colored :yellow "WARNING: ") args))))

(defn error [& args]
  (binding [*out* *err*]
    (println (apply str (colored :red "ERROR: ") args))))

(defn printer [& [opts]]
  (ddiff/printer (merge {:print-color *colored-output*} opts)))

(defn print-doc
  ([doc]
   (print-doc doc (printer)))
  ([doc printer]
   (fipp/pprint-document doc {:width (:width printer)})))

(defn format-doc
  ([doc]
   (format-doc doc (printer)))
  ([doc printer]
   (puget/format-doc printer doc)))

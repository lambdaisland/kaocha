(ns kaocha.output
  (:require [io.aviso.ansi :as ansi]))

(def ^:dynamic *colored-output* true)

(def colors
  {:black   ansi/black-font
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
    (println (apply str ansi/red-font "WARNING: " ansi/reset-font args))))

(defn error [& args]
  (binding [*out* *err*]
    (println (apply str ansi/red-font "ERROR: " ansi/reset-font args))))

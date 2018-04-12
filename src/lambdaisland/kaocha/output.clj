(ns lambdaisland.kaocha.output)

(def ^:dynamic *colored-output* true)

(def ESC \u001b)
(def CSI (str ESC \[))

(def colors [:black :red :green :yellow :blue :magenta :cyan :white])

(def fg* (into {} (map-indexed (fn [idx color] [color (str CSI (+ 30 idx) "m")]) colors)))
(def bg* (into {} (map-indexed (fn [idx color] [color (str CSI (+ 40 idx) "m")]) colors)))

(def reset (str CSI "0m"))
(def bold (str CSI "1m"))

(defn colored [c s]
  (if *colored-output*
    (str (fg* c) s reset)
    s))

(defn warn [& args]
  (binding [*out* *err*]
    (println (apply str (colored :red "WARNING: ") args))))

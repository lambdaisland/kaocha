(ns kaocha.plugin)

(def ^:dynamic *current-chain*)

;; TODO: duplicated from testable, not sure yet where to put it.
(defn- try-require [n]
  (try
    (require n)
    true
    (catch java.io.FileNotFoundException e
      false)))

(defn try-load-third-party-lib [type]
  (if (qualified-keyword? type)
    (when-not (try-require (symbol (str (namespace type) "." (name type))))
      (try-require (symbol (namespace type))))
    (try-require (symbol (name type)))))

(defmulti -register "Add your plugin to the stack"
  (fn [name plugins] name))

(defn register [name plugins]
  (try-load-third-party-lib name)
  (-register name plugins))

(defn load-all [names]
  (reduce #(register %2 %1) [] names))

(defn run-hook
  ([step value]
   (run-hook *current-chain* step value))
  ([plugins step value]
   (reduce #(%2 %1) value (keep step plugins))))

(comment
  (= (run-hook [{:foo inc} {:foo inc}] :foo 2)
     4))


;; HOOKS
;; :config
;; :pre-load
;; :post-load
;; :pre-run
;; :post-run

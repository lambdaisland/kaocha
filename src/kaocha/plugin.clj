(ns kaocha.plugin
  (:require [kaocha.output :as output]
            [clojure.string :as str]
            [slingshot.slingshot :refer [try+ throw+]]))

(def ^:dynamic *current-chain* [])

(defmacro with-plugins [chain & body]
  `(binding [*current-chain* ~chain] ~@body))

;; TODO: duplicated from testable, not sure yet where to put it.
(defn- try-require [n]
  (try
    (require n)
    true
    (catch java.io.FileNotFoundException e
      false)))

(defn try-load-third-party-lib [plugin-name]
  (if (qualified-keyword? plugin-name)
    (when-not (try-require (symbol (str (namespace plugin-name) "." (name plugin-name))))
      (try-require (symbol (namespace plugin-name))))
    (try-require (symbol (name plugin-name)))))

(defmulti -register "Add your plugin to the stack"
  (fn [name plugins] name))

(defmethod -register :default [name plugins]
  (output/error "Couldn't load plugin " name)
  (throw+ {:kaocha/early-exit 254} nil (str "Couldn't load plugin " name)))

(defn register [plugin-name plugin-stack]
  (let [plugin-name (if (and (simple-keyword? plugin-name)
                             (not (str/includes? "." (name plugin-name))))
                      ;; Namespaces without a period are not valid, we treat these as
                      ;; kaocha.plugin.*
                      (keyword "kaocha.plugin" (name plugin-name))
                      plugin-name)]
    (try-load-third-party-lib plugin-name)
    (-register plugin-name plugin-stack)))

(defn load-all [names]
  (reduce #(register %2 %1) [] (distinct names)))

(defn run-hook* [plugins step value & extra-args]
  (reduce (fn [value plugin]
            (if-let [step-fn (get plugin step)]
              (let [value (apply step-fn value extra-args)]
                (when (nil? value)
                  (output/warn "Plugin " (:kaocha.plugin/id plugin) " hook " step " returned nil."))
                value)
              value))
          value
          plugins))

(defn run-hook [step value & extra-args]
  (apply run-hook* *current-chain* step value extra-args))

(defmacro defplugin
  {:style/indent [1 :form [1]]}
  [id & hooks]
  (let [plugin-id (keyword id)
        var-sym (symbol (str (name id) "-hooks"))
        [desc & hooks] (if (string? (first hooks))
                         hooks
                         (cons "" hooks))]
    `(do
       ~@(map (fn [[hook & fn-tail]]
                `(defn ~(symbol (str (name id) "-" hook "-hook")) ~@fn-tail))
              hooks)

       (def ~var-sym
         ~(into {:kaocha.plugin/id plugin-id
                 :kaocha.plugin/description desc}
                (map (fn [[hook & _]]
                       [(keyword "kaocha.hooks" (str hook))
                        (symbol (str (name id) "-" hook "-hook"))]))
                hooks))
       (defmethod -register ~plugin-id [_# plugins#]
         (conj plugins# ~var-sym)))))

(comment
  (= (run-hook [{:foo inc} {:foo inc}] :foo 2)
     4))


;; HOOKS

;; :cli-options
;; :config
;; :pre-load
;; :post-load
;; :pre-run
;; :post-run
;; :wrap-run
;; :pre-test
;; :post-test
;; :pre-report

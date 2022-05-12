(ns kaocha.plugin.repl
  (:require 
    [clojure.main]
    [kaocha.plugin :as plugin :refer [defplugin]]
    [kaocha.result :as result])
  (:import [java.io OutputStreamWriter]))


(def ^:dynamic *testable* nil)

(comment (defn f [] (* 2 2) ))

(defn repl []
  (clojure.main/repl :init (fn []
                             (use 'clojure.repl)
                             (require '[ kaocha.plugin.repl :refer [ *testable*]]))
                     :prompt #(print "kaocha.plugin/repl=> ")))


(defplugin kaocha.plugin/repl
  (post-test [{:keys [actual] :as testable} _] 
             (when (result/erred? testable)
               (with-redefs [*out* (OutputStreamWriter. System/out)
                             *e actual
                             *testable* testable]
                 (println "Dropping into a repl! (via post-test)")
                 (repl)))
             testable)

  (wrap-run [run test-plan]
            (fn [& args]
              (try
                (apply run args)
                (catch Exception e 
                  (with-redefs [*out* (OutputStreamWriter. System/out)
                                *e e
                                ;; *testable* testable 
                                ]
                    (println "Dropping into a repl! (via wrap-run)")
                    (repl)))))))

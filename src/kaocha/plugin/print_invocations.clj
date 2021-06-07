(ns kaocha.plugin.print-invocations
  (:require [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [clojure.string :as str]))


;; This is an attempt to print out invocations that each re-run a single failing
;; test. The hard part is recreating a full command line invocation, which might
;; not be fully feasible.

(defplugin kaocha.plugin/print-invocations
  (post-summary [results]
    (when (result/failed? results)
      (println)
      (doseq [test (testable/test-seq results)]
        (if (and (not (seq (::result/tests test))) (result/failed? test))
          (let [id (str (::testable/id test))]
            (println "bin/kaocha"
                     (str/join
                      " "
                      (mapcat (fn [[k v]]
                                (cond
                                  (vector? v)
                                  (mapcat (fn [v] [(str "--" (name k)) v]) v)
                                  
                                  (true? v)
                                  [(str "--" (name k))]
                                  
                                  (false? v)
                                  [(str "--no-" (name k))]
                                  
                                  :else
                                  [(str "--" (name k))  v]))
                              (cond-> (dissoc (:kaocha/cli-options results) :focus)
                                (= "tests.edn" (:config-file (:kaocha/cli-options results)))
                                (dissoc :config-file))))
                     "--focus"
                     (str
                      "'" (cond-> id (= (first id) \:) (subs 1)) "'"))))))
    results))

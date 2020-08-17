(ns my.project.kaocha-hooks
  (:require [kaocha.testable :as testable]
            [kaocha.hierarchy :as hierarchy]
            [kaocha.result :as result]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; use as a post-summary hook
(defn blame-report [result]
  (let [clojure-test-suites (filter (comp #{:kaocha.type/clojure.test} :kaocha.testable/type)
                                    (:kaocha.result/tests result))]
    (doseq [suite clojure-test-suites
            ns-testable (:kaocha.result/tests suite)
            :when (result/failed? ns-testable)
            :let [ns-name (:kaocha.ns/name ns-testable)]]
      (println
       ns-name "last touched by"
       (re-find #"Author:.*"
                (:out
                 (sh/sh "git" "log" "-1" (str/replace (str (or (io/resource (str (.. (name ns-name)
                                                                                     (replace \- \_)
                                                                                     (replace \. \/))
                                                                                 ".clj"))
                                                               (io/resource (str (.. (name ns-name)
                                                                                     (replace \- \_)
                                                                                     (replace \. \/))
                                                                                 ".cljc"))))
                                                      "file:" "")))))))
  result)


(comment
  (require '[kaocha.repl :as repl]
           '[kaocha.api :as api])

  (def test-result (api/run (repl/config {})))

  (blame-report test-result)
  )

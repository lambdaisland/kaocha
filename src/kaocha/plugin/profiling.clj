(ns kaocha.plugin.profiling
  (:require [kaocha.plugin :as plugin :refer [defplugin]]
            [java-time :as jt]
            [kaocha.testable :as testable]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import java.time.Instant))

(defn start [testable]
  (assoc testable ::start (Instant/now)))

(defn stop [testable]
  (cond-> testable
    (::start testable)
    (assoc ::duration (jt/time-between (::start testable)
                                       (Instant/now)
                                       :nanos))))

(defplugin kaocha.plugin/profiling
  (pre-run [test-plan]
    (start test-plan))

  (post-run [test-plan]
    (stop test-plan))

  (pre-test [testable _]
    (start testable))

  (post-test [testable _]
    (stop testable))

  (config [config]
    (assoc config ::limit (::limit config 3)))

  (post-summary [result]
    (let [tests (remove :kaocha.test-plan/load-error (testable/test-seq result))
          types (group-by :kaocha.testable/type tests)
          total-dur (::duration result)
          limit (::limit result)]
      (->> (for [[type tests] types
                 :when type
                 :let [slowest (take limit (reverse (sort-by ::duration tests)))
                       slow-test-dur (apply + (keep ::duration slowest))]]
             [(format "\nTop %s slowest %s (%.5f seconds, %.1f%% of total time)\n"
                      (count slowest)
                      (subs (str type) 1)
                      (float (/ slow-test-dur 1e9))
                      (float (* (/ slow-test-dur total-dur) 100)))
              (for [test slowest
                    :let [duration (::duration test)
                          cnt (count (:kaocha.result/tests test))]
                    :when duration]
                (if (> cnt 0)
                  (format "  %s\n    \033[1m%.5f seconds\033[0m average (%.5f seconds / %d tests)\n"
                          (subs (str (:kaocha.testable/id test)) 1)
                          (float (/ duration cnt 1e9))
                          (float (/ duration 1e9))
                          cnt)

                  (when (:file (:kaocha.testable/meta test))
                    (format "  %s\n    \033[1m%.5f seconds\033[0m %s:%d\n"
                            (subs (str (:kaocha.testable/id test)) 1)
                            (float (/ duration 1e9))
                            (str/replace (:file (:kaocha.testable/meta test))
                                         (str (.getCanonicalPath (io/file ".")) "/")
                                         "")
                            (:line (:kaocha.testable/meta test))))))])
           (flatten)
           (apply str)
           print)
      (flush))
    result))

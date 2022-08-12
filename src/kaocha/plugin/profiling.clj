(ns kaocha.plugin.profiling
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.testable :as testable])
  (:import java.time.Instant
           java.time.temporal.ChronoUnit))

(s/def ::start #(instance? Instant %))
(s/def ::duration nat-int?)
(s/def ::profiling? boolean?)
(s/def ::count nat-int?)

(defn start [testable]
  (assoc testable ::start (Instant/now)))

(defn stop [testable]
  (cond-> testable
    (::start testable)
    (assoc ::duration (.until (::start testable)
                              (Instant/now)
                              ChronoUnit/NANOS))))

(defplugin kaocha.plugin/profiling
  (pre-run [test-plan]
    (start test-plan))

  (post-run [test-plan]
    (stop test-plan))

  (pre-test [testable _]
    (start testable))

  (post-test [testable _]
    (stop testable))

  (cli-options [opts]
    (conj opts
          [nil "--[no-]profiling"      "Show slowest tests of each type with timing information."]
          [nil "--profiling-count NUM" "Show this many slow tests of each kind in profile results."
           :parse-fn #(Integer/parseInt %)]))

  (config [{:kaocha/keys [cli-options] :as config}]
    (assoc config
           ::profiling? (:profiling cli-options (::profiling? config true))
           ::count      (:profiling-count cli-options (::count config 3))))

  (post-summary [result]
    (when (::profiling? result)
      (let [tests     (->> result
                           testable/test-seq
                           (remove ::testable/load-error)
                           (remove ::testable/skip))
            types     (group-by :kaocha.testable/type tests)
            total-dur (::duration result)
            limit     (::count result)]
        (->> (for [[type tests] types
                   :when        type
                   :let         [slowest (take limit (reverse (sort-by ::duration tests)))
                                 slow-test-dur (apply + (keep ::duration slowest))]]
               [(format "\nTop %s slowest %s (%.5f seconds, %.1f%% of total time)\n"
                        (count slowest)
                        (subs (str type) 1)
                        (float (/ slow-test-dur 1e9))
                        (float (* (/ slow-test-dur total-dur) 100)))
                (for [test  slowest
                      :let  [duration (::duration test)
                             cnt (count (remove ::testable/skip (:kaocha.result/tests test)))]
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
        (flush)))
    result))

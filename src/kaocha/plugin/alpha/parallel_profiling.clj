(ns kaocha.plugin.alpha.parallel-profiling
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.output :as output]
            [kaocha.testable :as testable])
  (:import java.time.Instant
           java.time.temporal.ChronoUnit))

(defn start [testable]
  (assoc testable ::start (Instant/now)))

(defn stop [testable]
  (cond-> testable
    true (assoc ::end (Instant/now))
    (::start testable)
    (assoc ::duration (.until (::start testable)
                              (Instant/now)
                              ChronoUnit/NANOS))))

(defplugin kaocha.plugin.alpha/parallel-profiling
  (config [config]
          (output/warn "Warning: The kaocha.plugin.alpha/parallel-profiling plugin is in an alpha status, like the parallel feature in general.")
          config
          )
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
          [nil "--[no-]-parallel-profiling"      "Show slowest tests of each type with timing information."]
          #_[nil "--profiling-count NUM" "Show this many slow tests of each kind in profile results."
           :parse-fn #(Integer/parseInt %)]))

  (config [{:kaocha/keys [cli-options] :as config}]
    (assoc config
           ::parallel-profiling? (:parallel-profiling cli-options (::parallel-profiling? config true))
           #_#_::count      (:profiling-count cli-options (::count config 3))))

  (post-summary [result]
    (when (::parallel-profiling? result)
      (let [tests     (->> result
                           testable/test-seq
                           (remove ::testable/load-error)
                           (remove ::testable/skip))
            #_#_types     (group-by :kaocha.testable/type tests)
            threads (group-by #(get-in % [ :kaocha.testable/thread :name]) tests)
            total-duration (::duration result)
            #_#_limit     (::count result)
            ]
        (->> (for [[thread tests] threads
                   :when        (and thread 
                                     (some (complement nil?) (map ::start tests)) 
                                     (some (complement nil?) (map ::end tests))) ;temporary fix until I figure out why these keys are sometimes missing.
                   :let         [start (reduce min (map ::start tests))
                                 end (reduce max (map ::end tests))
                                 span-ns (.until start
                                                 end
                                                 ChronoUnit/NANOS)
                                 span (cond 
                                            (> span-ns 1e8) (format "%.2f s" (/ span-ns 1e9))
                                            (> span-ns 1e5) (format "%.2f ms" (/ span-ns 1e9))
                                            :else (str span-ns " ns"))
                                 utilization (float (* 100 (/ (reduce + (map ::duration tests)) span-ns)))
                                 utilization-external (float (* 100 (/ (reduce + (map ::duration tests)) total-duration)))
                                 ]]

               (println (format "Thread %s ran from %s to %s (%s), utilizing %.2f%% (internal) and %.2f%% (external)" 
                                thread start end span utilization utilization-external)))
             (flatten)
             (apply str)
             print)


        (println (format "\n%d threads ran in %f seconds." (count threads) (float (/ total-duration 1e9))))
        (flush)

        ))
    result))

(.until (Instant/now) (Instant/now)  ChronoUnit/NANOS)

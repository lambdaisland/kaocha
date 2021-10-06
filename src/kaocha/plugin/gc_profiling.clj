(ns kaocha.plugin.gc-profiling
  (:require 
    [kaocha.testable :as testable]
            [kaocha.plugin :as plugin :refer [defplugin]]))


(def force-collection? false)

(defn convert-bytes [bytes]
  (let [abs-bytes (Math/abs bytes) ] 
    (cond
      (> abs-bytes 1e9) (format "%.2fGB" (/ bytes 1e9))
      (> abs-bytes 1e6) (format "%.2fMB" (/ bytes 1e6))
      (> abs-bytes 1e3) (format "%.2fKB" (/ bytes 1e3))
      :else (str bytes))))


(defn get-memory []
  (when force-collection?
    (System/gc)
    (System/runFinalization))

  (- (.totalMemory (Runtime/getRuntime))
     (.freeMemory (Runtime/getRuntime))))

(defn start [testable]
  (assoc testable ::start (get-memory)))

(defn stop [{::keys [start] :as testable}]
  (let [end (get-memory)  ]
    (assoc testable ::end (get-memory) ::delta (- end start))))


(defplugin kaocha.plugin/gc-profiling
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
                     [nil "--[no]memory-profiling" "Show the approximate memory used by each test."]) )

  (config [{:kaocha/keys [cli-options] :as config}]
          (assoc config
                 ::gc-profiling? (:gc-profiling cli-options (::gc-profiling? config false))
                 
                 ))

  (post-summary [result]
        (when (::gc-profilling? result)
          (let [tests     (->> result
                               testable/test-seq
                               (remove ::testable/load-error)
                               (remove ::testable/skip))
                types     (group-by :kaocha.testable/type tests) ]

            (doseq [t tests]
              (println (format "%-90s   %10s (%s)"
                               (:kaocha.testable/id t)
                               (convert-bytes (::delta t 0))
                               (:file (:kaocha.testable/meta t)))))
            result)))

  ;; (post-summary [result]
  ;;   (when (::profiling? result)
  ;;     (let [tests     (->> result
  ;;                          testable/test-seq
  ;;                          (remove ::testable/load-error)
  ;;                          (remove ::testable/skip))
  ;;           types     (group-by :kaocha.testable/type tests)
  ;;           total-dur (::duration result)
  ;;           limit     (::count result)]
  ;;       (->> (for [[type tests] types
  ;;                  :when        type
  ;;                  :let         [slowest (take limit (reverse (sort-by ::duration tests)))
  ;;                                slow-test-dur (apply + (keep ::duration slowest))]]
  ;;              [(format "\nTop %s slowest %s (%.5f seconds, %.1f%% of total time)\n"
  ;;                       (count slowest)
  ;;                       (subs (str type) 1)
  ;;                       (float (/ slow-test-dur 1e9))
  ;;                       (float (* (/ slow-test-dur total-dur) 100)))
  ;;               (for [test  slowest
  ;;                     :let  [duration (::duration test)
  ;;                            cnt (count (remove ::testable/skip (:kaocha.result/tests test)))]
  ;;                     :when duration]
  ;;                 (if (> cnt 0)
  ;;                   (format "  %s\n    \033[1m%.5f seconds\033[0m average (%.5f seconds / %d tests)\n"
  ;;                           (subs (str (:kaocha.testable/id test)) 1)
  ;;                           (float (/ duration cnt 1e9))
  ;;                           (float (/ duration 1e9))
  ;;                           cnt)
  ;;
  ;;                   (when (:file (:kaocha.testable/meta test))
  ;;                     (format "  %s\n    \033[1m%.5f seconds\033[0m %s:%d\n"
  ;;                             (subs (str (:kaocha.testable/id test)) 1)
  ;;                             (float (/ duration 1e9))
  ;;                             (str/replace (:file (:kaocha.testable/meta test))
  ;;                                          (str (.getCanonicalPath (io/file ".")) "/")
  ;;                                          "")
  ;;                             (:line (:kaocha.testable/meta test))))))])
  ;;            (flatten)
  ;;            (apply str)
  ;;            print)
  ;;       (flush)))
  ;;   result)
  
  )

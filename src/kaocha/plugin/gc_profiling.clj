(ns kaocha.plugin.gc-profiling
  (:require 
    [kaocha.output :as output]
    [kaocha.testable :as testable]
    [kaocha.hierarchy :as hierarchy]
    [kaocha.plugin :as plugin :refer [defplugin]]))


(def force-collection? false)

(defn convert-bytes [bytes]
  (let [abs-bytes (Math/abs bytes) ] 
    (cond
      (> abs-bytes 1e9) (format "%.2fGB" (/ bytes 1e9))
      (> abs-bytes 1e6) (format "%.2fMB" (/ bytes 1e6))
      (> abs-bytes 1e3) (format "%.2fkB" (/ bytes 1e3))
      :else (str bytes "B"))))


(defn get-memory []
  (when force-collection? ;try to force a collection
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
                    [nil "--[no-]gc-profiling" "Show the approximate memory used by each test."]
                    [nil "--[no-]gc-profiling-individual" "Show the details of individual tests."]))

  (config [{:kaocha/keys [cli-options] :as config}]
          (assoc config
                 ::gc-profiling? (:gc-profiling cli-options (::gc-profiling? config true))
                 ::gc-profiling-individual (:show-individual-tests-memory cli-options 
                                                                          (::gc-profiling-individual config false))))

  (post-summary [result]
        (when (::gc-profiling? result)
          (let [indentation-amount 2
                indentation-str (apply str (take 2 (repeat \space)))
                tests     (->> result
                               testable/test-seq
                               (remove ::testable/load-error)
                               (remove ::testable/skip)
                               (map #(update % :kaocha.testable/id name)))
                longest  (->> tests
                              (map :kaocha.testable/id) 
                              (map count)
                              (reduce (fn  
                                        ([] 0) ;in case of empty coll
                                        ([a b] (Math/max a b))))
                              (+ 2)) ;Leave space for identation
                types     (group-by :kaocha.testable/type tests)
               negative-allocations? (some neg? (remove nil? (map ::delta (testable/test-seq result))))]

            (when (::gc-profiling-individual result)
              (doseq [t tests
                      :let [leaf? (hierarchy/leaf? t)
                            padding (if leaf? (- longest indentation-amount) longest)] ]
                (println (format (str (when leaf? indentation-str) "%-" padding "s   %10s (%s)")
                                 (:kaocha.testable/id t)
                                 (convert-bytes (::delta t 0))
                                 (:file (:kaocha.testable/meta t))))))
            (doseq [[type tests] types
                    :when type
                    :let [largest (take 5 (reverse (sort-by ::delta tests)))
                          large-test-total (apply + (keep ::delta largest))]] ;no need to remove nil? here: keep takes care of that
              (println (format "\nTop 5 %s for memory usage. (Used %s, %.1f%% of total)"
                               type
                               (convert-bytes large-test-total)
                               (float (* (/ large-test-total (float (::delta result 1))) 100))))
              (doseq [{:keys [::delta :kaocha.testable/id] :as test} largest
                      :let [n (count (remove ::testable/skip (:kaocha.result/tests test)))]]
                (cond 
                  (> n 0) (let [avg (int (/ delta  n))]
                            (println (format "%s%s    \n%s\033[1m%s\033[0m average (%s / %d tests)" indentation-str 
                                             id (str indentation-str indentation-str)
                                             (convert-bytes avg) (convert-bytes delta) n)))
                  :else  (println (format "%s%s    \n%s\033[1m%s\033[0m " indentation-str 
                                          id (str indentation-str indentation-str)
                                          (convert-bytes delta))))))
            (when negative-allocations?
              (println (output/colored :yellow "\nWARNING:") "Some results are negative. This happens when more memory is freed than was allocated during a test. Try rerunning the test suite."))))
        result))

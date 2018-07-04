(ns kaocha.plugin.profiling
  (:require [kaocha.plugin :as plugin]
            [java-time :as jt]
            [kaocha.testable :as testable]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import java.time.Instant))


(defmethod plugin/-register :kaocha.plugin/profiling [_ plugins]
  (conj plugins
        {:kaocha.plugin/id :kaocha.plugin/profiling

         :kaocha.hooks/pre-test
         (fn [testable]
           (assoc testable ::start (Instant/now)))

         :kaocha.hooks/post-test
         (fn [testable]
           (cond-> testable
             (::start testable)
             (assoc ::duration (jt/time-between (::start testable)
                                                (Instant/now)
                                                :nanos))))

         :kaocha.hooks/post-run
         (fn [result]
           (let [tests (testable/test-seq result)
                 types (group-by :kaocha.testable/type tests)]
             (doseq [[type tests] types
                     :let [slowest (take 3 (reverse (sort-by ::duration tests)))]]
               (when type
                 (println "Slowest" type)
                 (doseq [test slowest
                         :let [duration (::duration test)
                               cnt (count (:kaocha.result/tests test))]]
                   (when duration
                     (if (> cnt 0)
                       (println
                        (format "  %s\n    \033[1m%.5f seconds\033[0m average (%.5f seconds / %d tests)"
                                (subs (str (:kaocha.testable/id test)) 1)
                                (float (/ duration cnt 1e9))
                                (float (/ duration 1e9))
                                cnt))

                       (println
                        (format "  %s\n    \033[1m%.5f seconds\033[0m %s:%d"
                                (subs (str (:kaocha.testable/id test)) 1)
                                (float (/ duration 1e9))
                                (str/replace (:file (:kaocha.testable/meta test))
                                             (str (.getCanonicalPath (io/file ".")) "/")
                                             "")
                                (:line (:kaocha.testable/meta test)))) ))
                   )
                 (println))))
           result)}))

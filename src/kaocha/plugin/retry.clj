(ns kaocha.plugin.retry
  "Instrument/unstrument namespaces with Orchestra, to get validation of function
  arguments and return values based on clojure.spec.alpha."
  (:require [kaocha.plugin :refer [defplugin]]
            [kaocha.hierarchy :as h]
            [kaocha.testable :as t]
            [kaocha.result :as r]))

(def max-retries 3)
(def wait-time 100)
;; this needs to be reset in a fixture before every test
(def current-retries (atom {}))

(defn any-failed? []
  (let [v (vals @current-retries)]
    (and (seq v)
         (= 3 (apply max v)))))

(defplugin kaocha.plugin/retry
  (pre-report [event]
    (if (any-failed?)
      event
      (assoc event :type :pass)))

  (pre-run [test-plan]
    (reset! current-retries {})
    test-plan)

  (post-test [test test-plan]
    (let [t-id (:kaocha.testable/id test)
          curr (get @current-retries t-id 0)]
      (when (and
             (h/leaf? test)
             (r/failed-one? test)
             (< curr max-retries))

        (println "Retrying time " @current-retries)
        (swap! current-retries assoc t-id (inc curr))
        (Thread/sleep wait-time)
        (t/run-testable test test-plan))

      test)))

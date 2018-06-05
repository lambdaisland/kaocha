(ns kaocha.api
  "Programmable test runner interface."
  (:require [kaocha.monkey-patch]
            [kaocha.testable :as testable]
            [kaocha.result :as result]
            [kaocha.plugin :as plugin]))

(defmacro ^:private with-reporter [r & body]
  `(with-redefs [t/report ~r]
     ~@body))

(defn run* [config]
  (let [{:kaocha/keys [reporter
                       color?
                       suites
                       tests
                       fail-fast?
                       plugins]} config
        plugins                  (plugin/load-all (:plugins config))
        config                   (plugin/run-step plugins :kaocha.hooks/config config)


        reporter                 (config/resolve-reporter
                                  (if fail-fast?
                                    [reporter report/fail-fast]
                                    reporter))
        stack                    (atom [config])
        runtime                  (java.lang.Runtime/getRuntime)
        main-thread              (Thread/currentThread)
        on-shutdown              (Thread. (fn []
                                            (println "^C")
                                            #_(binding [report/*results* results]
                                                (t/do-report (assoc (result->report @results)
                                                                    :type :summary)))))
        do-finish                (fn [report]
                                   (t/do-report (assoc report :type :summary))
                                   (.removeShutdownHook runtime on-shutdown)
                                   report)]
    (.addShutdownHook runtime on-shutdown)
    (try
      (binding [output/*colored-output* color?
                testable/*stack*        stack
                report/*results*        results]
        (with-reporter reporter
          (-> tests testable/load-testables testable/run-testables)))
      (finally
        (.removeShutdownHook runtime on-shutdown)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

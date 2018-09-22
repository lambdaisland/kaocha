(ns kaocha.watch
  (:require [hawk.core :as hawk]
            [kaocha.api :as api]
            [kaocha.result :as result]
            [clojure.core.async :refer [chan <!! put! poll!]]
            [clojure.java.io :as io]
            [lambdaisland.tools.namespace.dir :as ctn-dir]
            [lambdaisland.tools.namespace.file :as ctn-file]
            [lambdaisland.tools.namespace.parse :as ctn-parse]
            [lambdaisland.tools.namespace.reload :as ctn-reload]
            [lambdaisland.tools.namespace.track :as ctn-track]
            [clojure.string :as str]
            [clojure.set :as set]
            [kaocha.testable :as testable]
            [kaocha.stacktrace :as stacktrace]
            [kaocha.output :as out]
            [clojure.test :as t]))

(defn- try-run [config]
  (let [result (try
                 (api/run config)
                 (catch Throwable t
                   (println "[watch] Fatal error in test run" t)))]
    (println)
    result))

(defn run [config]
  (let [watch-paths       (into #{} (comp (remove :kaocha.testable/skip)
                                          (map (juxt :kaocha/test-paths :kaocha/source-paths))
                                          cat
                                          cat
                                          (map io/file))
                                (:kaocha/tests config))
        watch-chan        (chan)
        drain-watch-chan! (fn [] (doall (take-while identity (repeatedly #(poll! watch-chan)))))
        tracker           (-> (ctn-track/tracker)
                              (ctn-dir/scan-dirs watch-paths)
                              (dissoc :lambdaisland.tools.namespace.track/unload
                                      :lambdaisland.tools.namespace.track/load))]
    (future
      (try
        (loop [tracker tracker
               focus   nil]
          (let [unload     (set (::ctn-track/unload tracker))
                load       (set (::ctn-track/load tracker))
                load-error (::ctn-file/load-error tracker)
                reload     (set/intersection unload load)
                unload     (set/difference unload reload)
                load       (set/difference load reload)
                tracker    (ctn-reload/track-reload (assoc tracker ::ctn-file/load-error {}))]
            (when (seq load-error)
              (doseq [[f e] load-error]
                (out/warn "Failed loading" f)
                (stacktrace/print-cause-trace e t/*stack-trace-depth*)))

            (when (seq unload) (println "[watch] Unloading" unload))
            (when (seq load) (println "[watch] Loading" unload))
            (when (seq reload) (println "[watch] Reloading" reload))
            (when (seq focus) (println "[watch] Re-running failed tests" (set focus)))

            (let [config' (cond-> config (seq focus) (assoc :kaocha.filter/focus focus))
                  result  (try-run config')]
              (if (and (seq focus) (not (result/failed? result)))
                (do
                  (println "[watch] Failed tests pass, re-running all tests.")
                  (drain-watch-chan!)
                  (recur (ctn-dir/scan-dirs tracker) nil))
                (let [f (<!! watch-chan)]
                  (drain-watch-chan!)
                  (recur (ctn-dir/scan-dirs tracker)
                         (->> result
                              testable/test-seq
                              (filter result/failed-one?)
                              (map ::testable/id))))))))
        (catch Throwable t
          (.printStackTrace t))
        (finally
          (println "[watch] loop broken"))))

    (hawk/watch! [{:paths   watch-paths
                   :handler (fn [ctx event]
                              (when (= (:kind event) :modify)
                                (put! watch-chan (:file event))))}])

    @(promise)))

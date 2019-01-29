(ns kaocha.watch
  (:refer-clojure :exclude [symbol])
  (:require [hawk.core :as hawk]
            [kaocha.api :as api]
            [kaocha.result :as result]
            [kaocha.plugin :refer [defplugin]]
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
            [kaocha.output :as output]
            [clojure.test :as t]
            [kaocha.plugin :as plugin]
            [kaocha.config :as config]
            [kaocha.core-ext :refer :all])
  (:import [java.nio.file FileSystems]
           [java.util.concurrent ArrayBlockingQueue BlockingQueue]))

(defn make-queue []
  (ArrayBlockingQueue. 1024))

(defn qput [^BlockingQueue q x]
  (.put q x))

(defn qpoll [^BlockingQueue q]
  (.poll q))

(defn qtake [^BlockingQueue q]
  (.take q))

(defn drain-queue! [q]
  (doall (take-while identity (repeatedly #(qpoll q)))))

(defn- try-run [config focus tracker]
  (let [config (if (seq focus)
                 (assoc config :kaocha.filter/focus focus)
                 config)
        config (-> config
                   (assoc ::focus focus)
                   (assoc ::tracker tracker)
                   (update :kaocha/plugins #(cons ::plugin %)))
        result (try
                 (api/run config)
                 (catch Throwable t
                   (println "[watch] Fatal error in test run" t)))]
    (println)
    result))

(defn track-reload! [tracker]
  (ctn-reload/track-reload (assoc tracker ::ctn-file/load-error {})))

(defn print-scheduled-operations! [tracker focus]
  (let [unload (set (::ctn-track/unload tracker))
        load   (set (::ctn-track/load tracker))
        reload (set/intersection unload load)
        unload (set/difference unload reload)
        load   (set/difference load reload)]
    (when (seq unload)
      (println "[watch] Unloading" unload))
    (when (seq load)
      (println "[watch] Loading" load))
    (when (seq reload)
      (println "[watch] Reloading" reload))
    (when (seq focus)
      (println "[watch] Re-running failed tests" (set focus)))))

(defn drain-and-rescan! [q tracker watch-paths]
  (drain-queue! q)
  (ctn-dir/scan-dirs tracker watch-paths))

(defn glob?
  "Does path match any of the glob patterns.

  See [FileSystem/getPathMatcher](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
  for a description of the patterns, these are similar but not the same as
  typical shell glob patterns. "
  [path patterns]
  (let [fs (FileSystems/getDefault)
        patterns (map #(.getPathMatcher fs (str "glob:" %)) patterns)]
    (some #(.matches % path) patterns)))

(defn wait-and-rescan! [q tracker watch-paths ignore]
  (let [f (qtake q)]
    (cond
      (and (file? f) (glob? (.toPath f) ignore))
      (recur q tracker watch-paths ignore)

      (= :finish f)
      [tracker f]

      :else
      [(drain-and-rescan! q tracker watch-paths) f])))

(defn reload-config [config]
  (let [{:kaocha/keys [cli-options cli-args]} config
        {:keys [config-file plugin]}          cli-options

        config       (-> config-file
                         (config/load-config)
                         (config/apply-cli-opts cli-options)
                         (config/apply-cli-args cli-args))
        plugin-chain (plugin/load-all (concat (:kaocha/plugins config) plugin))]
    [config plugin-chain]))

(defn run-loop [finish? config tracker q watch-paths]
  (loop [tracker      tracker
         config       config
         plugin-chain plugin/*current-chain*
         focus        nil]
    (when-not @finish?
      (let [result  (try-run config focus tracker)
            tracker (::tracker result)
            error   (::error result)
            ignore  (::ignore config)]

        (cond
          error
          (do
            (println "[watch] Error reloading, all tests skipped.")
            (let [[tracker _] (wait-and-rescan! q tracker watch-paths ignore)
                  [config plugin-chain] (reload-config config)]
              (recur tracker config plugin-chain nil)))

          (and (seq focus) (not (result/failed? result)))
          (do
            (println "[watch] Failed tests pass, re-running all tests.")
            (recur (drain-and-rescan! q tracker watch-paths) config plugin-chain nil))

          :else
          (let [[tracker trigger] (wait-and-rescan! q tracker watch-paths ignore)
                [config plugin-chain] (reload-config config)
                focus (when-not (= :enter trigger)
                        (->> result
                             testable/test-seq
                             (filter result/failed-one?)
                             (map ::testable/id)))]
            (recur tracker config plugin-chain focus)))))))

(defplugin kaocha.watch/plugin
  "This is an internal plugin, don't use it directly.

Behind the scenes we add this plugin to the start of the plugin chain. It takes
care of reloading namespaces inside a Kaocha run, so we can report any load
errors as test errors."
  (pre-load [{::keys [tracker focus] :as config}]
    (print-scheduled-operations! tracker focus)
    (let [tracker    (track-reload! tracker)
          config     (assoc config ::tracker tracker)
          error      (::ctn-reload/error tracker)
          error-ns   (::ctn-reload/error-ns tracker)
          load-error (::ctn-file/load-error tracker)]
      (if (and error error-ns)
        (-> config
            (assoc ::error error ::error-ns error-ns)
            (update :kaocha/tests
                    (partial map (fn [t] (assoc t :kaocha.testable/skip true)))))
        config)))

  (pre-test [test test-plan]
    (if-let [error (::error test-plan)]
      (do
        (t/do-report {:type :error
                      :kaocha/testable {:kaocha.testable/id :kaocha/watch}
                      :message (str "Failed reloading ns: " (::error-ns test-plan))
                      :actual error})
        (assoc test
               :kaocha.result/count 1
               :kaocha.result/error 1
               ::testable/skip-remaining? true))
      test)))

(defn watch-paths [config]
  (into #{}
        (comp (remove :kaocha.testable/skip)
              (map (juxt :kaocha/test-paths :kaocha/source-paths))
              cat
              cat
              (map io/file))
        (:kaocha/tests config)))

(defn watch! [q watch-paths]
  (hawk/watch! [{:paths   watch-paths
                 :handler (fn [ctx event]
                            (when (= (:kind event) :modify)
                              (qput q (:file event))))}]))

(defn run* [config finish? q]
  (let [watch-paths (watch-paths config)
        tracker     (-> (ctn-track/tracker)
                        (ctn-dir/scan-dirs watch-paths)
                        (dissoc :lambdaisland.tools.namespace.track/unload
                                :lambdaisland.tools.namespace.track/load))]

    (watch! q watch-paths)
    (watch! q [(get-in config [:kaocha/cli-options :config-file])])

    (future
      (let [stdin (io/reader System/in)]
        (while (not @finish?)
          (if (and (.ready stdin) (= (.read stdin) (long \newline)))
            (qput q :enter)
            (Thread/sleep 100)))))

    (try
      (run-loop finish? config tracker q watch-paths)
      (catch Throwable t
        (.printStackTrace t))
      (finally
        (println "[watch] watching stopped.")))))

(defn run [config]
  (let [finish? (atom false)
        q       (make-queue)]
    (future
      (run* config finish? q))
    (fn []
      (reset! finish? true)
      (qput q :finish))))

(ns kaocha.watch
  (:refer-clojure :exclude [symbol])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.spec.alpha :as spec]
            [clojure.stacktrace :as st]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [kaocha.api :as api]
            [kaocha.config :as config]
            [kaocha.core-ext :refer :all]
            [kaocha.output :as output]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.plugin :as plugin]
            [kaocha.result :as result]
            [kaocha.testable :as testable]
            [kaocha.util :as util]
            [lambdaisland.tools.namespace.dir :as ctn-dir]
            [lambdaisland.tools.namespace.file :as ctn-file]
            [lambdaisland.tools.namespace.reload :as ctn-reload]
            [lambdaisland.tools.namespace.track :as ctn-track]
            [slingshot.slingshot :refer [try+]]
            [nextjournal.beholder :as beholder])
  (:import (java.nio.file FileSystems)
           (java.util.concurrent ArrayBlockingQueue BlockingQueue)
           (io.methvin.watcher DirectoryWatcher)
           (io.methvin.watcher.hashing FileHasher)))

(defn nomis-emit-hacked-version-message []
  (println "\u001b[35;1m[watch] You are using Simon's hacked Kaocha\u001b[0m" ))

(def nomis-no-focus? true)

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
  (let [focus  (if nomis-no-focus? nil focus)
        config (if (seq focus)
                 (assoc config :kaocha.filter/focus focus)
                 config)
        config (-> config
                   (assoc ::focus focus)
                   (assoc ::tracker tracker)
                   (update :kaocha/plugins #(cons ::plugin %)))
        result (try
                 (plugin/run-hook :kaocha.hooks/post-summary (api/run config))
                 (catch Throwable t
                   (println "[watch] Fatal error in test run" t)))]
    (println)
    result))

(defn track-reload! [{::keys [tracker-error] :as tracker}]
  (cond-> (assoc tracker ::ctn-file/load-error {})
    (not tracker-error) ctn-reload/track-reload))

(defn print-scheduled-operations! [tracker focus]
  (nomis-emit-hacked-version-message)
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

(defn- circular-dependency? [e]
  (= :lambdaisland.tools.namespace.dependency/circular-dependency
     (:reason (ex-data e))))

(defn drain-and-rescan! [q tracker watch-paths]
  (drain-queue! q)
  (try (ctn-dir/scan-dirs tracker watch-paths)
       (catch clojure.lang.ExceptionInfo e
         (if (circular-dependency? e)
           (assoc tracker ::tracker-error e)
           (throw e)))))

(defn glob?
  "Does path match any of the glob patterns.

  See [FileSystem/getPathMatcher](https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String))
  for a description of the patterns, these are similar but not the same as
  typical shell glob patterns."
  [path patterns]
  (let [fs (FileSystems/getDefault)
        patterns (map #(.getPathMatcher fs (str "glob:" %)) patterns)]
    (some #(.matches % path) patterns)))

(defn convert
  "Converts a Git-style ignore pattern into the equivalent pattern that Java PathMatcher uses."
  [pattern]
  (let [cleaned  (-> pattern
                     ;; If a Git pattern has a trailing space, it should be stripped (unless escaped)
                     ;; Example: 'src/test ' => 'src/test'
                     (str/replace #"([^\\]) +$" "$1")

                     ;; If a Git pattern contains a double star bordering no path
                     ;; separators, it basically functions as a single star, AFAICT:
                     ;; Per the man page: "Other consecutive asterisks are
                     ;; considered regular asterisks and will match according to the
                     ;; previous rules." ("other consecutive asterisks" = not
                     ;; preceded, followed, or surounded by separators)
                     ;;
                     (str/replace #"[^/][*][*][^/]" "*")

                     ;; If a Git pattern contains a double star between path
                     ;; separators, that means zero or more intervening directories.
                     ;;(Java treats this as at least one directory because the path
                     ;; separators are interpreted literally.)
                     ;; Exmple: 'src/**/test'  => 'src**test'
                     (str/replace #"/[*][*]/" "**")


                     ;; If a Git pattern contains braces, those should be treated literally
                     ;; Example: src/{ill-advised-filename}.clj => src/\{ill-advised-filename\}.clj
                     ;; (re-find #"[{}]" pattern) (str/replace pattern #"\{(.*)\}" "\\\\{$1\\\\}"  )
                     (str/replace #"\{(.*)\}" "\\\\{$1\\\\}"))]
    (cond->> cleaned
      ;; If it starts with a single *, it should have **
      ;; Example: *.html => **.html
      (re-find #"^\*[^*]" cleaned) (format "*%s")

      ;; If a Git pattern ends with a slash, that represents everything underneath
      ;; Example: src/ => src/**
      (re-find #"/$" cleaned) (format "%s**")

      ;; Otherwise, it should have the same behavior
)))

(spec/fdef convert :args (spec/cat :pattern string?) :ret string?)

(defn parse-ignore-file
  "Parses an individual ignore file."
  [file]
  (->> (slurp file)
       (str/split-lines)
       ;; filter out comments, which need to be ignored, and negated patterns, which need to be handled separately:
       (filter #(not (re-find #"^[!#]" %)))
       (map #(convert %))))

(defn find-ignore-files
  "Finds ignore files in the local directory and the system."
  [dir]
  (let [absolute-files [(io/file (str (System/getProperty "user.home") "/.config/git/ignore"))]
        relative-files (filter #(glob? (.toPath %) ["**.gitignore" "**.ignore"]) (file-seq (io/file dir)))]
    (into absolute-files relative-files)))

(defn merge-ignore-files
  "Combines and parses ignore files."
  [dir]
  (let [all-files  (find-ignore-files dir)]
    (mapcat #(when (.exists (io/file %)) (parse-ignore-file %)) all-files)))

(spec/fdef merge-ignore-files
  :args (spec/cat :dir string?)
  :ret (spec/coll-of string?))

(defn wait-and-rescan! [q tracker watch-paths ignore]
  (let [f (qtake q)]
    (cond
      (and (file? f) (glob? (.toPath f) ignore))
      (recur q tracker watch-paths ignore)

      (directory? f)
      (recur q tracker watch-paths ignore)

      (= :finish f)
      [tracker f]

      :else
      [(drain-and-rescan! q tracker watch-paths) f])))

(defn reload-config [config plugin-chain]
  (if-let [config-file (get-in config [:kaocha/cli-options :config-file])]
    (let [{:kaocha/keys [cli-options cli-args]} config
          profile (get-in config [:kaocha/cli-options :profile])
          config (try+
                  (-> (config/load-config (config/find-config-and-warn config-file) {:profile profile})
                      (config/apply-cli cli-options cli-args)
                      (config/validate!))
                  (catch :kaocha/early-exit e
                    (output/warn "Error loading config: " e "\nFalling back to prior config.")))
          plugin-chain (plugin/load-all (concat (:kaocha/plugins config) (:plugin cli-options)))]
      [config plugin-chain])
    [config plugin-chain]))

(defn run-loop [finish? config tracker q watch-paths]
  (loop [tracker      tracker
         config       config
         plugin-chain plugin/*current-chain*
         focus        nil]
    (when-not @finish?
      (let [result  (try-run config focus tracker)
            tracker (dissoc (::tracker result) ::tracker-error)
            error?  (::error? result)
            ignore  (if (::use-ignore-file config)
                      (into (::ignore config)
                            (merge-ignore-files "."))
                      (::ignore config))]
        (cond
          error?
          (do
            (println "[watch] Error reloading, all tests skipped.")
            (let [[tracker _] (wait-and-rescan! q tracker watch-paths ignore)
                  [config plugin-chain] (reload-config config plugin-chain)]
              (recur tracker config plugin-chain nil)))

          (and (not nomis-no-focus?) (seq focus) (not (result/failed? result)))
          (do
            (println "[watch] Failed tests pass, re-running all tests.")
            (recur (drain-and-rescan! q tracker watch-paths) config plugin-chain nil))

          :else
          (let [[tracker trigger] (wait-and-rescan! q tracker watch-paths ignore)
                [config plugin-chain] (reload-config config plugin-chain)
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
          error      (or (::tracker-error tracker)
                         (::ctn-reload/error tracker))
          error-ns   (if (circular-dependency? error)
                       (:node (ex-data error))
                       (::ctn-reload/error-ns tracker))]
      (if (and error error-ns)
        (let [[file line] (util/compiler-exception-file-and-line error)]
          (-> config
              (assoc ::error? true)
              (update :kaocha/tests
                      (fn [suites]
                        ;; We don't really know which suite the load error
                        ;; belongs to, it could well be in a file shared by all
                        ;; suites, so we arbitrarily put the load error on the
                        ;; first non-skipped suite and skip all others, so that
                        ;; it gets reported properly.
                        (let [applied? (volatile! false)]
                          (mapv (fn [suite]
                                  (if (and (not @applied?)
                                           (not (::testable/skip suite)))
                                    (do (vreset! applied? true)
                                        (assoc suite
                                               ::testable/load-error error
                                               ::testable/load-error-file (or file (util/ns-file error-ns))
                                               ::testable/load-error-line line
                                               ::testable/load-error-message (str "Failed reloading " error-ns ":")))
                                    (assoc suite ::testable/skip true)))
                                suites))))))
                config))))

(defn watch-paths [config]
  (into #{}
        (comp (remove :kaocha.testable/skip)
              (map (juxt :kaocha/test-paths :kaocha/source-paths))
              cat
              cat
              (map io/file))
        (:kaocha/tests config)))

(defmulti watch! :type)

(defmethod watch! :hawk [{:keys [q watch-paths opts]}]
  (hawk/watch! opts
               [{:paths   watch-paths
                 :handler (fn [ctx event]
                            (when (= (:kind event) :modify)
                              (qput q (:file event))))}]))

(defn- nomis-beholder-create
  "Hacked version of beholder/create"
  [cb paths]
  (-> (DirectoryWatcher/builder)
      (.paths (map @#'beholder/to-path paths))
      (.listener (@#'beholder/fn->listener cb))
      (.fileHasher FileHasher/LAST_MODIFIED_TIME)
      (.build)))

(defn- nomis-beholder-watch
  "Hacked version of beholder/watch"
  [cb & paths]
  (doto (nomis-beholder-create cb paths)
    (.watchAsync)))

(defmethod watch! :beholder [{:keys [q watch-paths]}]
  (apply nomis-beholder-watch
         (fn [{:keys [type path]}]
           (when (contains? #{:modify :create} type)
             (qput q path)))
         (map str watch-paths)))

(defn run* [config finish? q]
  (let [watcher-type (::type config :beholder)
        watcher-opts (condp = watcher-type
                       :hawk (::hawk-opts config {})
                       :beholder {} ;; beholder does not take opts
                       {})
        watch-paths (if (:kaocha.watch/use-ignore-file config)
                      (set/union (watch-paths config)
                                 (set (map #(.getParentFile (.getCanonicalFile %)) (find-ignore-files "."))))
                      (watch-paths config))
        tracker (ctn-track/tracker)
        ;; if t.n fails due to circular dependencies, do not track-reload.
        ;; instead, report the failure as a compilation error. repeat this
        ;; strategy each time around run-loop.
        tracker (try (-> tracker
                         (ctn-dir/scan-dirs watch-paths)
                         (dissoc :lambdaisland.tools.namespace.track/unload
                                 :lambdaisland.tools.namespace.track/load))
                     (catch clojure.lang.ExceptionInfo e
                       (if (circular-dependency? e)
                         (assoc tracker ::tracker-error e)
                         (throw e))))]

    (when (or (= watcher-type :hawk) (::hawk-opts config))
      (output/warn "Hawk watcher is deprecated in favour of Beholder. Kaocha will soon get rid of Hawk completely."))

    (watch! {:type watcher-type
             :q q
             :watch-paths watch-paths
             :opts watcher-opts})
    (when-let [config-file (get-in config [:kaocha/cli-options :config-file])]
      (when (and (= watcher-type :hawk) ;;Only Hawk supports watching single files.
                 (.exists (io/file config-file)))
        (watch! {:type watcher-type
                 :q q
                 :watch-paths #{config-file}
                 :opts watcher-opts})))

    (future
      (let [stdin (io/reader System/in)]
        (while (not @finish?)
          (if (and (.ready stdin) (= (.read stdin) (long \newline)))
            (qput q :enter)
            (Thread/sleep 100)))))

    (run-loop finish? config tracker q watch-paths)))

(defn run [config]
  (nomis-emit-hacked-version-message)
  (let [finish?   (atom false)
        q         (make-queue)
        finish!   (fn []
                    (reset! finish? true)
                    (qput q :finish))
        bfn       (bound-fn []
                    (try
                      (run* config finish? q)
                      0
                      (catch Throwable t
                        (st/print-cause-trace t)
                        -3)
                      (finally
                        (println "[watch] watching stopped."))))
        exit-code (future (bfn))]
    [exit-code finish!]))

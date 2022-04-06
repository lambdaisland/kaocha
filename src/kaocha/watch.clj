(ns kaocha.watch
  (:refer-clojure :exclude [symbol])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.stacktrace :as st]
            [clojure.string :as str]
            [clojure.test :as t]
            [hawk.core :as hawk]
            [kaocha.api :as api]
            [kaocha.config :as config]
            [kaocha.core-ext :refer :all]
            [kaocha.load :as load]
            [kaocha.output :as output]
            [kaocha.plugin :refer [defplugin]]
            [kaocha.plugin :as plugin]
            [kaocha.result :as result]
            [kaocha.stacktrace :as stacktrace]
            [kaocha.testable :as testable]
            [kaocha.watch-deps :as watch-deps]
            [kaocha.util :as util]
            [lambdaisland.tools.namespace.dir :as ctn-dir]
            [lambdaisland.tools.namespace.file :as ctn-file]
            [lambdaisland.tools.namespace.parse :as ctn-parse]
            [lambdaisland.tools.namespace.reload :as ctn-reload]
            [lambdaisland.tools.namespace.track :as ctn-track]
            [nextjournal.beholder :as beholder])
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

(defn- try-run [config fail-focus tracker]
  (let [config (if (seq fail-focus)
                 (assoc config :kaocha.filter/focus fail-focus)
                 config)
        config (-> config
                   (assoc ::fail-focus fail-focus)
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

(defn tracker-changes [tracker]
  (let [unload (set (::ctn-track/unload tracker))
        load   (set (::ctn-track/load tracker))
        reload (set/intersection unload load)]
    {:load   (set/difference load reload)
     :unload (set/difference unload reload)
     :reload reload}))

(defn print-scheduled-operations! [{:keys [unload load reload]} fail-focus]
  (when (seq unload)
    (println "[watch] Unloading" unload))
  (when (seq load)
    (println "[watch] Loading" load))
  (when (seq reload)
    (println "[watch] Reloading" reload))
  (when (seq fail-focus)
    (println "[watch] Re-running failed tests" (set fail-focus))))

(defn drain-and-rescan! [q tracker watch-paths]
  (drain-queue! q)
  (ctn-dir/scan-dirs tracker watch-paths))

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

(s/fdef convert :args (s/cat :pattern string?) :ret string?)

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
        relative-files (filter #(glob? (.toPath %) ["**.gitignore" "**.ignore"] ) (file-seq (io/file dir)))]
    (into absolute-files relative-files)))

(defn merge-ignore-files
  "Combines and parses ignore files."
  [dir]
  (let [all-files  (find-ignore-files dir)]
    (mapcat #(when (.exists (io/file %)) (parse-ignore-file %)) all-files )))

(s/fdef merge-ignore-files
  :args (s/cat :dir string?)
  :ret (s/coll-of string?))

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
          config (-> config-file
                     (config/load-config)
                     (config/apply-cli-opts cli-options)
                     (config/apply-cli-args cli-args))
          plugin-chain (plugin/load-all (concat (:kaocha/plugins config) (:plugin cli-options)))]
      [config plugin-chain])
    [config plugin-chain]))

(defn run-loop [finish? config q watch-paths]
  (let [tracker (-> (ctn-track/tracker)
                    (ctn-dir/scan-dirs watch-paths)
                    (dissoc :lambdaisland.tools.namespace.track/unload
                            :lambdaisland.tools.namespace.track/load))]
    (loop [tracker      tracker
           config       config
           plugin-chain plugin/*current-chain*
           fail-focus   nil
           first?       true]
      (when-not @finish?
        (let [result  (try-run config fail-focus tracker)
              tracker (::tracker result)
              error?  (::error? result)
              ignore  (if (::use-ignore-file config)
                        (into (::ignore config)
                              (merge-ignore-files "."))
                        (::ignore config))]
          (when first?
            (watch-deps/init! (:lambdaisland.tools.namespace.file/filemap tracker)))
          (cond
            error?
            (do
              (println "[watch] Error reloading, all tests skipped.")
              (let [[tracker _] (wait-and-rescan! q tracker watch-paths ignore)
                    [config plugin-chain] (reload-config config plugin-chain)]
                (recur tracker config plugin-chain nil false)))

            (and (seq fail-focus) (not (result/failed? result)))
            (do
              (println "[watch] Failed tests pass, re-running all tests.")
              (recur (drain-and-rescan! q tracker watch-paths) config plugin-chain nil false))

            :else
            (let [[tracker trigger] (wait-and-rescan! q tracker watch-paths ignore)
                  [config plugin-chain] (reload-config config plugin-chain)
                  fail-focus (when-not (= :enter trigger)
                               (->> result
                                    testable/test-seq
                                    (filter result/failed-one?)
                                    (map ::testable/id)))]
              (recur tracker config plugin-chain fail-focus false))))))))

(defplugin kaocha.watch/plugin
  "This is an internal plugin, don't use it directly.

Behind the scenes we add this plugin to the start of the plugin chain. It takes
care of reloading namespaces inside a Kaocha run, so we can report any load
errors as test errors."
  (pre-load [{::keys [tracker fail-focus] :as config}]
    (let [changes (tracker-changes tracker)]
      (print-scheduled-operations! changes fail-focus)
      (let [tracker    (track-reload! tracker)
            ;; TODO: handle unload
            change-focus (when-let [last-changes (->> (select-keys changes [:reload :load])
                                                      vals
                                                      (map (partial into []))
                                                      flatten
                                                      seq)]
                           (reduce (fn [acc ns]
                                     (let [f (some (fn [[k v]] (when (= v ns) k))
                                                   (:lambdaisland.tools.namespace.file/filemap tracker))]
                                       (concat acc (watch-deps/update-file! f ns))))
                                   []
                                   last-changes))
            ;; TODO: when focus is empty, don't run any tests
            focus      (concat fail-focus change-focus)
            config     (assoc config
                              ::tracker tracker
                              :kaocha.filter/focus focus)
            error      (::ctn-reload/error tracker)
            error-ns   (::ctn-reload/error-ns tracker)
            load-error (::ctn-file/load-error tracker)]
        (when (seq change-focus)
          (println "[watch] Running tests affected by changes" (set change-focus)))
      (if (and error error-ns)
        (let [[file line] (util/compiler-exception-file-and-line error)]
          (-> config
              (assoc ::error? true)
              (update :kaocha/tests
                      (fn [suites]
                        ;; We don't really know which suite the load error
                        ;; belongs to, it could well be in a file shared by all
                        ;; suites, so we arbitrarily put the load error on the
                        ;; first and skip the rest, so that it gets reported
                        ;; properly.
                        (into [(assoc (first suites)
                                      ::testable/load-error error
                                      ::testable/load-error-file (or file (util/ns-file error-ns))
                                      ::testable/load-error-line line
                                      ::testable/load-error-message (str "Failed reloading " error-ns ":"))]
                              (map #(assoc % ::testable/skip true))
                              (rest suites))))))
        config)))))

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

(defmethod watch! :beholder [{:keys [q watch-paths]}]
  (apply beholder/watch
         (fn [{:keys [type path]}]
           (when (= type :modify)
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
                      (watch-paths config))]

    (when (or (= watcher-type :hawk) (::hawk-opts config))
      (output/warn "Hawk watcher is deprecated in favour of beholder. Kaocha will soon get rid of hawk completely."))

    (watch! {:type watcher-type
             :q q
             :watch-paths watch-paths
             :opts watcher-opts})
    (when-let [config-file (get-in config [:kaocha/cli-options :config-file])]
      (when (.exists (io/file config-file))
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

    (run-loop finish? config q watch-paths)))

(defn run [config]
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

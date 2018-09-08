(ns kaocha.watch
  (:require [hawk.core :as hawk]
            [kaocha.api :as api]
            [kaocha.result :as result]
            [clojure.core.async :refer [chan <!! put! poll!]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.file :as ctn-file]
            [clojure.tools.namespace.parse :as ctn-parse]
            [clojure.string :as str]
            [kaocha.testable :as testable]))

(defn- ns-file-name [sym ext]
  (let [base (-> (name sym)
                 (str/replace "-" "_")
                 (str/replace "." java.io.File/separator))]
    (str base "." ext)))

(defn reload-file! [f]
  (try
    (let [ns      (->> f ctn-file/read-file-ns-decl ctn-parse/name-from-ns-decl)
          ext     (last (str/split (str f) #"\."))
          ns-file (io/resource (ns-file-name ns ext))]
      (when (contains? #{"clj" "cljc"} ext)
        (if (= (str ns-file) (str (.toURL f)))
          (do
            (println "Reloading" ns)
            (require ns :reload))
          (println
           (if ns-file
             (str "Not reloading " ns ", it resolves to " ns-file " instead of " f)
             (str "Not reloading " ns ", the namespace does not match the file name " f))))))
    (catch Throwable t
      (.printStackTrace t))))

(defn- try-run [config]
  (try
    (api/run config)
    (catch Throwable t
      (println "Fatal error in test run" t))))

(defn run [config]
  (let [watch-paths (into #{} (comp (remove :kaocha.testable/skip)
                                    (map (juxt :kaocha/test-paths :kaocha/source-paths))
                                    cat
                                    cat
                                    (map io/file))
                          (:kaocha/tests config))
        watch-chan  (chan)]
    (future
      (try
        (loop [reload []
               focus  nil]
          (run! reload-file! reload)
          (when (seq focus)
            (println "Focusing on failed tests:" (str/join ", " focus)))
          (let [config' (cond-> config (seq focus) (assoc :kaocha.filter/focus focus))
                result (try-run config')]
            (if (and (seq focus) (not (result/failed? result)))
              (do
                (println "Failed tests pass, re-running all tests.")
                (recur (take-while identity (repeatedly #(poll! watch-chan))) nil))
              (let [f (<!! watch-chan)]
                (recur (into #{} (cons f (take-while identity (repeatedly #(poll! watch-chan)))))
                       (->> result
                            testable/test-seq
                            (filter result/failed-one?)
                            (map ::testable/id)))))))
        (catch Throwable t
          (.printStackTrace t))
        (finally
          (println "loop broken"))))
    (hawk/watch! [{:paths   watch-paths
                   :handler (fn [ctx event]
                              (when (= (:kind event) :modify)
                                (put! watch-chan (:file event))))}])
    (Thread/sleep Long/MAX_VALUE)))

(ns kaocha.watch
  (:require [hawk.core :as hawk]
            [kaocha.config :as config]
            [clojure.core.async :refer [chan <!! put! poll!]]
            [kaocha.test :as test]
            [clojure.java.io :as io]
            [clojure.tools.namespace.file :as ctn.file]
            [clojure.tools.namespace.parse :as ctn.parse]
            [clojure.string :as str]))

(defn- ns-file-name [sym ext]
  (let [base (-> (name sym)
                 (str/replace "-" "_")
                 (str/replace "." java.io.File/separator))]
    (str base "." ext)))

(defn reload-file! [f]
  (try
    (let [ns      (->> f ctn.file/read-file-ns-decl ctn.parse/name-from-ns-decl)
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

(defn run [config]
  (let [{:kaocha/keys [suites only-suites]
         :as   config} (config/normalize config)
        suites         (config/filter-suites only-suites suites)
        watch-paths    (into #{} (comp
                                  (map (juxt :kaocha/test-paths :kaocha/source-paths))
                                  cat
                                  cat
                                  (map io/file))
                             suites)
        watch-chan     (chan)]
    (future
      (try
        (loop [reload []]
          (run! reload-file! reload)
          (test/run config)
          (let [f (<!! watch-chan)]
            (recur (into #{} (cons f (take-while identity (repeatedly #(poll! watch-chan))))))))
        (catch Throwable t
          (.printStackTrace t))
        (finally
          (println "loop broken"))))
    (hawk/watch! [{:paths watch-paths
                   :handler (fn [ctx event]
                              (when (= (:kind event) :modify)
                                (put! watch-chan (:file event))))}])
    (Thread/sleep Long/MAX_VALUE)))

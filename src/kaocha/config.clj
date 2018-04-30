(ns kaocha.config
  "Read,validate, normalize configuration as found in tests.edn or passed in
  through command line options."
  (:require [clojure.java.io :as io]
            [kaocha.output :as out]
            [kaocha.report :as report]
            [slingshot.slingshot :refer [throw+]]))

(def global-opts #{:kaocha/reporter
                   :kaocha/color?
                   :kaocha/randomize?
                   :kaocha/seed
                   :kaocha/suites
                   :kaocha/only-suites
                   :kaocha/fail-fast?
                   :kaocha/watch?})

(def suite-opts #{:kaocha/id
                  :kaocha/source-paths
                  :kaocha/test-paths
                  :kaocha/ns-patterns})

(def cli-opts {:test-path   :kaocha/test-paths
               :ns-pattern  :kaocha/ns-patterns
               :source-path :kaocha/source-paths

               :watch       :kaocha/watch?
               :fail-fast   :kaocha/fail-fast?
               :suites      :kaocha/suites
               :color       :kaocha/color?
               :randomize   :kaocha/randomize?
               :seed        :kaocha/seed
               :reporter    :kaocha/reporter
               :only-suites :kaocha/only-suites})

(defn default-config []
  (read-string (slurp (io/resource "kaocha/default_config.edn"))))

(defn load-config
  ([]
   (load-config "tests.edn"))
  ([path]
   (let [file (io/file path)]
     (if (.exists file)
       (let [body (slurp file)]
         (if (seq body)
           (read-string body)
           nil))
       (out/warn "Config file not found: " path ", using default values.")))))

(defn- rename-key [m old-key new-key]
  (if (contains? m old-key)
    (assoc (dissoc m old-key) new-key (get m old-key))
    m))

(defn normalize-cli-opts [opts]
  (reduce (fn [opts [k v]]
            (rename-key opts k v))
          opts
          cli-opts))

(defn filter-suites [suite-ids suites]
  (if (seq suite-ids)
    (filter #(some #{(:kaocha/id %)} suite-ids) suites)
    suites))

(defn fn->multifn [fn]
  "Coerce a regular function into a multimethod. Needed because tools like
  matcher-combinators and earlier versions of test.check assume that
  clojure.test/report is always bound to a mutlimethod."
  (if (instance? clojure.lang.MultiFn fn)
    fn
    (doto (clojure.lang.MultiFn. (str "fn->multifn<" fn ">") (constantly :default) :default #'clojure.core/global-hierarchy)
      (.addMethod :default fn))))

(defn resolve-reporter [reporter]
  (cond
    (= 'clojure.test/report reporter)
    report/clojure-test-report

    (symbol? reporter)
    (do
      (try
        (if-let [ns (namespace reporter)]
          (require (symbol ns))
          (throw+ {:kaocha/reporter-not-found reporter}))
        (catch java.io.FileNotFoundException e
          (throw+ {:kaocha/reporter-not-found reporter})))
      (if-let [resolved (resolve reporter)]
        (resolve-reporter @resolved)
        (throw+ {:kaocha/reporter-not-found reporter})))

    (seqable? reporter)
    (let [rs (doall (map resolve-reporter reporter))]
      (fn->multifn (fn [m] (run! #(% m) rs))))

    :else
    reporter))

(defn normalize [config]
  (let [config (merge (default-config) config)
        global (select-keys config global-opts)
        suite  (select-keys config suite-opts)]
    (-> global
        (update :kaocha/suites (fn [suites]
                                 (->> suites
                                      (mapv (partial merge suite))))))))

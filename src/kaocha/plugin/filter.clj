(ns kaocha.plugin.filter
  (:refer-clojure :exclude [symbol])
  (:require [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.testable :as testable]
            [clojure.set :as set]
            [kaocha.output :as output]
            [kaocha.core-ext :refer [symbol]]))

(defn- accumulate [m k v]
  (update m k (fnil conj []) v))

(defn matches? [{:as testable
                 ::testable/keys [id meta]}
                filters meta-filters]
  (or (some #(= (keyword %) id) filters)
      (some #(= (str %) (namespace id)) filters)
      (some #(get meta (keyword %)) meta-filters)))

(defn filters [{:as testable
                :kaocha.filter/keys [skip focus skip-meta focus-meta]}]
  {:skip skip
   :focus focus
   :skip-meta skip-meta
   :focus-meta focus-meta})

(defn merge-filters [f1 f2]
  {:skip       (concat (:skip f1) (:skip f2))
   :skip-meta  (concat (:skip-meta f1) (:skip-meta f2))
   :focus      (if (seq (:focus f2))
                 (:focus f2)
                 (:focus f1))
   :focus-meta (if (seq (:focus-meta f2))
                 (:focus-meta f2)
                 (:focus-meta f1))})

(defn truthy-keys [m]
  (map key (filter val m)))

(defn remove-missing-metadata-keys [focus-meta testable]
  (let [used-meta  (into #{}
                         (comp
                          (map (comp truthy-keys ::testable/meta))
                          cat
                          (map keyword))
                         (testable/test-seq testable))
        focus-meta (set focus-meta)
        unused     (set/difference focus-meta used-meta)]
    (doseq [u unused]
      (output/warn "No tests found with metadata key " u ". Ignoring --focus-meta " u "."))
    (set/difference focus-meta unused)))

(defn filter-testable [testable opts]
  (let [{:as opts
         :keys [skip focus skip-meta focus-meta]} (merge-filters opts (filters testable))
        recurse   (fn recurse
                    ([]
                     (recurse opts))
                    ([opts]
                     (cond-> testable
                       (:kaocha.test-plan/tests testable)
                       (update :kaocha.test-plan/tests (partial map #(filter-testable % opts))))))
        skip-test (fn []
                    (assoc testable ::testable/skip true))]

    (cond
      (or (seq focus) (seq focus-meta))
      (cond
        (matches? testable focus focus-meta)
        (recurse (dissoc opts :focus :focus-meta))

        (some #(matches? % focus focus-meta) (testable/test-seq testable))
        (recurse)

        :else
        (skip-test))

      (matches? testable skip skip-meta)
      (skip-test)

      (:kaocha.test-plan/tests testable)
      (recurse)

      :else
      testable)))

(defplugin kaocha.plugin/filter
  (cli-options [opts]
    (let [parse #(keyword (if (= \: (first %)) (subs % 1) %))]
      (conj opts
            [nil "--skip SYM" "Skip tests with this ID and their children."
             :parse-fn parse
             :assoc-fn accumulate]
            [nil "--focus SYM" "Only run this test, skip others."
             :parse-fn parse
             :assoc-fn accumulate]
            [nil "--skip-meta SYM" "Skip tests where this metadata key is truthy."
             :parse-fn parse
             :assoc-fn accumulate]
            [nil "--focus-meta SYM" "Only run tests where this metadata key is truthy."
             :parse-fn parse
             :assoc-fn accumulate])))

  (config [config]
    (let [{:keys [skip focus skip-meta focus-meta]} (:kaocha/cli-options config)]
      (when (and (or (seq focus)
                     (seq focus-meta)
                     (seq skip)
                     (seq skip-meta))
                 (> (count (:kaocha/tests config)) 1))
        (println "Multiple test id available and --focus/--focus-meta/--skip/--skip-meta specified")
        (System/exit 1))

      (cond-> config
        (seq skip)       (assoc :kaocha.filter/skip skip)
        (seq focus)      (assoc :kaocha.filter/focus focus)
        (seq skip-meta)  (assoc :kaocha.filter/skip-meta skip-meta)
        (seq focus-meta) (assoc :kaocha.filter/focus-meta focus-meta))))

  (post-load [test-plan]
    (let [{:keys [focus focus-meta]} (:kaocha/cli-options test-plan)
          test-plan (update test-plan :kaocha.filter/focus-meta remove-missing-metadata-keys test-plan)
          filter-suite (fn [suite]
                         (filter-testable
                          (if (or (seq focus) (seq focus-meta))
                            (dissoc suite :kaocha.filter/focus :kaocha.filter/focus-meta)
                            (update suite :kaocha.filter/focus-meta remove-missing-metadata-keys test-plan))
                          (filters test-plan)))]
      (-> test-plan
          (update :kaocha.test-plan/tests (partial map filter-suite))))))

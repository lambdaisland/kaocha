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
                 ::testable/keys [id meta aliases]}
                filters meta-filters]
  (or (some #(= (keyword %) id) filters)
      (some #(= (str %) (namespace id)) filters)
      (seq (filter (set aliases) (map keyword filters)))
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
        (::testable/load-error testable)
        testable

        ;; - positive
        ;;   - foo ^:a
        ;;   - bar ^:b
        ;; --focus positive --focus-meta a
        ;;
        (matches? testable focus focus-meta)
        (recurse (dissoc opts :focus :focus-meta))

        ;; Is it better to split this?
        ;; e.g. focus-meta inside

        ;; (matches? testable focus #{})
        ;; (recurse (dissoc opts :focus))

        ;; (matches? testable #{} focus-meta)
        ;; (recurse (dissoc opts :focus-meta))

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
      (cond-> config
        (seq skip)       (assoc :kaocha.filter/skip skip)
        (seq focus)      (assoc :kaocha.filter/focus focus)
        (seq skip-meta)  (assoc :kaocha.filter/skip-meta skip-meta)
        (seq focus-meta) (assoc :kaocha.filter/focus-meta focus-meta))))

  ;; In an earlier pass already filter at the test suite level. We don't have
  ;; the full test plan yet, but if a suite itself matches any of the focus/skip
  ;; directives then we can already apply that, thus possibly preventing the
  ;; loading of suites that won't run in this test run anyway.
  (pre-load [config]
    (let [{:kaocha.filter/keys [focus focus-meta skip skip-meta]} config
          focus-suites (->> config
                            :kaocha/tests
                            (filter #(matches? % focus focus-meta))
                            (map :kaocha.testable/id)
                            set)]
      (update config
              :kaocha/tests
              (fn [suites]
                (mapv (fn [suite]
                        (if (and
                             (not (:kaocha.testable/skip suite)) ; short circuit if this has been set elsewhere, saves a few cycles
                             (or (matches? suite skip skip-meta)
                                 (and (seq focus-suites)
                                      (not (contains? focus-suites (:kaocha.testable/id suite))))))
                          (assoc suite :kaocha.testable/skip true)
                          suite))
                      suites)))))

  (post-load [test-plan]
    (let [{:kaocha.filter/keys [focus focus-meta]} (:kaocha/cli-options test-plan)]
      (when (and (seq focus) (empty? (filter #(matches? % focus nil) (testable/test-seq test-plan))))
        (output/warn ":focus " focus " did not match any tests."))
      (let [test-plan (update test-plan :kaocha.filter/focus-meta remove-missing-metadata-keys test-plan)
            filter-suite (fn [suite]
                           ;; Suites may already be marked as skipped in
                           ;; kaocha.config when procssing CLI arguments, in
                           ;; that case don't do any further processing, e.g. we
                           ;; don't want to emit warnings about missing metadata
                           ;; keys.
                           (if (:kaocha.testable/skip suite)
                             suite
                             (let [suite (update suite :kaocha.filter/focus-meta remove-missing-metadata-keys test-plan)]
                               (-> suite
                                   (filter-testable {})
                                   (dissoc :kaocha.filter/focus :kaocha.filter/focus-meta)
                                   (filter-testable (filters test-plan))))))]
        (-> test-plan
            (update :kaocha.test-plan/tests (partial map filter-suite)))))))

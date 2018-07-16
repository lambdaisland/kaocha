(ns kaocha.plugin.filter
  (:require [kaocha.plugin :as plugin :refer [defplugin]]
            [kaocha.testable :as testable]))

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
    (let [parse #(symbol (if (= \: (first %)) (subs % 1) %))]
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

  (post-load [test-plan]
    (let [{:keys [focus focus-meta]} (:kaocha/cli-options test-plan)
          filter-suite (fn [suite]
                         (filter-testable
                          (cond-> suite
                            (or (seq focus) (seq focus-meta))
                            (dissoc :kaocha.filter/focus :kaocha.filter/focus-meta))
                          (filters test-plan)))]
      (update test-plan :kaocha.test-plan/tests (partial map filter-suite)))))

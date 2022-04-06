(ns kaocha.watch-deps
  "Analysis that tracks depedencies between forms + a hash of the forms.
  Enables calculating which tests need to rerun after a form changes"
  (:require [nextjournal.clerk.hashing :as hashing]
            [clojure.set :as set]))

(defn info [file]
  (let [analyzed-doc (-> file
                         hashing/parse-file
                         hashing/build-graph)]
    (assoc (:graph analyzed-doc) :->hash (->> (hashing/hash analyzed-doc)
                                              (filter (fn [[k _v]] (symbol? k)))
                                              (into {})))))

(defn test-vars-for-file [ns]
  ;; TODO this only works for things like clojure.test; need to find the kaocha
  ;; way of gettings test vars per ns
  (into #{}
        (filter #(-> % meta :test)
                (vals (ns-interns ns)))))

(defonce !state (atom {:files {}
                       :test-vars #{}}))

(defn init!
  "for a map from ns to file-handler, calculate the depedency analysis for each ns"
  [filemap]
  (reset! !state
          (reduce (fn [acc [file ns]]
                    (let [filepath (str file)]
                      (-> acc
                          (update :test-vars #(set/union % (test-vars-for-file ns)))
                          (assoc-in [:files filepath] (info filepath)))))
                  {:files {}
                   :test-vars #{}}
                  filemap)))

(defn affected-test-symbols [all-test-vars all-deps last-info new-info]
  (let [all-test-syms (into #{} (map symbol all-test-vars))
        changed (->> last-info
                     :->hash
                     (filter (fn [[k v]] (not= (get-in new-info [:->hash k]) v)))
                     (map first))]
    (->> changed
         (reduce (fn [test-vars changed-var]
                   (set/union test-vars
                              (set/intersection all-test-syms
                                                (get all-deps changed-var))))
                 #{})
         (into #{}))))

(defn update-file!
  "re-analyze a file and return symbols for test variables that are affected by
  changes"
  [file ns]
  (let [filename      (str file)
        prev-analysis (get-in @!state [:files filename])
        test-vars     (set/union (test-vars-for-file ns)
                                 (get @!state :test-vars))
        new-analysis  (info filename)
        all-deps      (apply merge-with
                             set/union
                             (map (comp :dependents second) (:files @!state)))]
    (if (= prev-analysis new-analysis)
      []
      (do (swap! !state
                 #(-> %
                      (assoc :test-vars test-vars)
                      (assoc-in [:files filename] new-analysis)))
          (affected-test-symbols test-vars all-deps prev-analysis new-analysis)))))
